package com.everyplaceinkorea.epik_boot3_api.auth.util;

import com.everyplaceinkorea.epik_boot3_api.auth.entity.EpikUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JwtUtil {

    private final Key secretKey;
    private final long accessTokenExpirationTime = 1000 * 60 * 60 * 10; // 10시간
    private final long refreshTokenExpirationTime = 1000 * 60 * 60 * 24 * 7; // 7일

    public JwtUtil(@Value("${epik.jwt.secret}") String secret) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return !claims.getExpiration().before(new Date());

        } catch (JwtException | IllegalArgumentException e) {
            log.error("잘못되거나 만료된 JWT 토큰: {}", e.getMessage());
            return false;
        }
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).get("email", String.class);
    }

    public List<String> extractRoles(String token) {
        try {
            Object rolesObj = extractAllClaims(token).get("role");
            if(rolesObj == null) {
                return Collections.emptyList();
            }

            // role이 List<String>인 경우 (저장 방식)
            if(rolesObj instanceof List) {
                List<?> rolesList = (List<?>) rolesObj;
                return rolesList.stream()
                        .map(role -> {
                            if(role instanceof String) {
                                return (String) role;
                            } else if(role instanceof Map) {
                                Map<?, ?> roleMap = (Map<?, ?>) role;
                                return (String) roleMap.get("authority");
                            }
                            return null;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }

            if(rolesObj instanceof String) {
                return Collections.singletonList((String) rolesObj);
            }

            log.warn("예상하지 못한 role 타입: {}", rolesObj.getClass());
            return Collections.emptyList();

        } catch(Exception e) {
            log.error("Role 추출 중 오류 발생: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public Long extractId(String token) {
        return extractAllClaims(token).get("id", Long.class);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // UserDetails 객체로부터 JWT 토큰 생성
    public String generateToken(UserDetails userDetails) {
        EpikUserDetails epikUserDetails = (EpikUserDetails) userDetails;

        // 사용자 권한 정보 추출 - List<String>으로 저장
        List<String> roles = epikUserDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        // 토큰 생성 시간 및 만료 시간 설정
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpirationTime);

        log.debug("토큰 생성 - username: {}, id: {}, roles: {}",
                epikUserDetails.getUsername(),
                epikUserDetails.getId(),
                roles);

        // 토큰에 포함할 클레임(정보) 설정
        return Jwts.builder()
                .setSubject(epikUserDetails.getUsername())
                .claim("id", epikUserDetails.getId())
                .claim("username", epikUserDetails.getUsername())
                .claim("email", epikUserDetails.getEmail())
                .claim("nickname", epikUserDetails.getNickname())
                .claim("role", roles)
                .claim("profileImg", epikUserDetails.getProfileImage())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    // JWT 리프레시 토큰 생성
    public String generateRefreshToken(EpikUserDetails userDetails) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpirationTime);

        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .claim("id", userDetails.getId())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }
}
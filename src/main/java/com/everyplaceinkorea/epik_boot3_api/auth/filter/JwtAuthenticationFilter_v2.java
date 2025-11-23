package com.everyplaceinkorea.epik_boot3_api.auth.filter;

import com.everyplaceinkorea.epik_boot3_api.auth.entity.EpikUserDetails;
import com.everyplaceinkorea.epik_boot3_api.auth.util.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter_v2 extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // 쿠키에서 JWT 토큰 추출
        String token = extractTokenFromCookie(request);

        // 쿠키에 없으면 Authorization 헤더 확인
        if(token == null || token.isEmpty()) {
            token = extractTokenFromHeader(request);
        }

        // 토큰이 존재하지 않으면 다음 필터로
        if (token == null || token.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 토큰이 있으면 유효성 검증 후 인증 설정
            if (jwtUtil.validateToken(token)) {

                String username = jwtUtil.extractUsername(token);
                Long id = jwtUtil.extractId(token);
                String email = jwtUtil.extractEmail(token);
                List<String> roles = jwtUtil.extractRoles(token);

                // roles 리스트를 SimpleGrantedAuthority로 변환
                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                // EpikUserDetails 객체 생성
                UserDetails userDetails = EpikUserDetails.builder()
                        .id(id)
                        .username(username)
                        .email(email)
                        .authorities(authorities)
                        .build();

                // Spring Security 인증 토큰 생성 및 SecurityContext 설정
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (ExpiredJwtException e) {
            // 토큰 만료
            log.warn("만료된 JWT 토큰: {}", e.getMessage());
            response.setHeader("X-Auth-Error", "TOKEN_EXPIRED");

        } catch (SignatureException e) {
            // 서명 검증 실패 (위조된 토큰)
            log.warn("유효하지 않은 JWT 서명: {}", e.getMessage());
            response.setHeader("X-Auth-Error", "INVALID_SIGNATURE");

        } catch (MalformedJwtException e) {
            // 잘못된 토큰 형식
            log.warn("잘못된 형식의 JWT 토큰: {}", e.getMessage());
            response.setHeader("X-Auth-Error", "MALFORMED_TOKEN");

        } catch (UnsupportedJwtException e) {
            // 지원하지 않는 토큰
            log.warn("지원하지 않는 JWT 토큰: {}", e.getMessage());
            response.setHeader("X-Auth-Error", "UNSUPPORTED_TOKEN");

        } catch (IllegalArgumentException e) {
            // 빈 토큰
            log.warn("JWT 토큰이 비어있습니다: {}", e.getMessage());
            response.setHeader("X-Auth-Error", "EMPTY_TOKEN");

        } catch (Exception e) {
            // 그 외 예상치 못한 오류
            log.error("예상치 못한 JWT 검증 오류: {}", e.getMessage(), e);
            response.setHeader("X-Auth-Error", "UNEXPECTED_ERROR");
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 쿠키에서 "jwt_token"이라는 이름을 가진 쿠키의 값을 찾아 반환.
     * @param request HttpServletRequest
     * @return jwt_token 값 또는 null
     */
    private String extractTokenFromCookie(HttpServletRequest request){
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("jwt_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }

    private String extractTokenFromHeader(HttpServletRequest request){
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}




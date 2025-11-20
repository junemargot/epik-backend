package com.everyplaceinkorea.epik_boot3_api.auth.filter;

import com.everyplaceinkorea.epik_boot3_api.auth.entity.EpikUserDetails;
import com.everyplaceinkorea.epik_boot3_api.auth.util.JwtUtil;
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
        } catch (Exception e) {
            log.error("JWT 토큰 검증 중 오류 발생: {}", e.getMessage());
            // 검증에 실패하면 그냥 인증 없이 넘어감 Anonymous 상태로 남음
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




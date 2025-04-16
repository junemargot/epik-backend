package com.everyplaceinkorea.epik_boot3_api.auth.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2LogoutSuccessHandler implements LogoutSuccessHandler {

  @Override
  public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

    // JWT 토큰 쿠키 삭제
    ResponseCookie cookie = ResponseCookie.from("jwt_token", "")
            .httpOnly(true)
            .secure(true)
            .path("/")
            .maxAge(0)
            .build();

    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

    // 프론트엔드 로그아웃 페이지로 리다이렉트
    response.sendRedirect("http://localhost:3000/");
  }
}

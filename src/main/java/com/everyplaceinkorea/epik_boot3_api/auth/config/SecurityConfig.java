package com.everyplaceinkorea.epik_boot3_api.auth.config;

import com.everyplaceinkorea.epik_boot3_api.auth.filter.JwtAuthenticationFilter_v2;
import com.everyplaceinkorea.epik_boot3_api.auth.handler.OAuth2AuthenticationSuccessHandler;
import com.everyplaceinkorea.epik_boot3_api.auth.handler.OAuth2LogoutSuccessHandler;
import com.everyplaceinkorea.epik_boot3_api.auth.service.CustomOAuth2UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final CustomOAuth2UserService customOAuth2UserService;
  private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
  private final OAuth2LogoutSuccessHandler oAuth2LogoutSuccessHandler;
  private final JwtAuthenticationFilter_v2 jwtAuthenticationFilter;

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
    return authenticationConfiguration.getAuthenticationManager();
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {
    http
            // CSRF 보호 비활성화
            .csrf(AbstractHttpConfigurer::disable)
            // CORS 설정 적용
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            // 세션 관리 설정 (JWT 사용으로 STATELESS 모드)
            .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 요청 권한 설정
            .authorizeHttpRequests(authorize -> authorize
                    .requestMatchers("/auth/**", "/login/**", "/images/**", "/uploads/**", "/api/v1/uploads/**",
                                    "/oauth2/**", "/oauth2/authorization/**", "/login/oauth2/code/**",
                                    "/popup/random", "/concert/random", "/musical/random", "/exhibition/random",
                                    "/admin/kopis/**", "/admin/dashboard/**").permitAll() // KOPIS 관리 API 허용 추가
                    .requestMatchers("/api/v1/admin/**").hasAuthority("ROLE_ADMIN")
                    .requestMatchers(HttpMethod.GET, "/feed/**").permitAll()  // 피드 조회는 비로그인 허용
                    .requestMatchers(HttpMethod.POST, "/feed/**").authenticated()  // 피드 작성, 좋아요는 인증 필요
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .anyRequest().authenticated()
            )

            .exceptionHandling(exception -> exception
                    .authenticationEntryPoint((request, response, authException) -> {
                      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                      response.setContentType("application/json");
                      response.getWriter().write("{\"error\": \"Unauthorized\"}");
                    })
            )

            // 인증방식 설정
            .formLogin(form -> form
                    .loginPage("/login")
                    .permitAll()
            )
            // OAuth2 로그인 설정
            .oauth2Login(oauth2 -> oauth2
                    .loginPage("/login")
                    .userInfoEndpoint(userInfo -> userInfo
                            .userService(customOAuth2UserService)
                    ).successHandler(oAuth2AuthenticationSuccessHandler)
            )

            // 로그아웃 설정
            .logout(logout -> logout
                    .logoutUrl("/api/v1/auth/logout")
                    .logoutSuccessHandler(oAuth2LogoutSuccessHandler)
                    .deleteCookies("jwt_token") // 로그아웃 시 쿠키 삭제
                    .invalidateHttpSession(true)
            )

            // h2 콘솔 사용 설정
            .headers(headers -> headers
                    .frameOptions(frameOptions -> frameOptions.sameOrigin()))

            // JWT 필터 추가
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.addAllowedOriginPattern("*");
    config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.addAllowedHeader("*");
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
package com.everyplaceinkorea.epik_boot3_api.util;

import com.everyplaceinkorea.epik_boot3_api.auth.entity.EpikUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Spring Security를 통해 현재 인증된 사용자 정보를 가져오는 유틸리티 클래스
 */
@Component
public class SecurityUtil {

    /**
     * 현재 로그인한 사용자의 ID(memberId)를 반환
     *
     * @return 로그인한 사용자의 memberId
     * @throws IllegalStateException 인증되지 않은 경우
     */
    public static Long getCurrentMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 인증 정보가 없거나 익명 사용자인 경우
        if(authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("인증되지 않은 사용자입니다.");
        }

        Object principal = authentication.getPrincipal();

        // "anonymousUser" 문자열인 경우 (비로그인 상태)
        if("anonymousUser".equals(principal)) {
            throw new IllegalStateException("인증되지 않은 사용자입니다.");
        }

        // EpikUserDetails에서 memberId 추출
        if(principal instanceof EpikUserDetails) {
            EpikUserDetails userDetails = (EpikUserDetails) principal;
            return userDetails.getId();
        }

        throw new IllegalStateException("사용자 정보를 찾을 수 없습니다.");
    }

    /**
     * 현재 로그인한 사용자의 이메일을 반환
     *
     * @return 로그인한 사용자의 이메일
     * @throws IllegalStateException 인증되지 않은 경우
     */
    public static String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("인증되지 않은 사용자입니다.");
        }

        Object principal = authentication.getPrincipal();

        if ("anonymousUser".equals(principal)) {
            throw new IllegalStateException("인증되지 않은 사용자입니다.");
        }

        if (principal instanceof EpikUserDetails) {
            EpikUserDetails userDetails = (EpikUserDetails) principal;
            return userDetails.getEmail();
        }

        throw new IllegalStateException("사용자 정보를 찾을 수 없습니다.");
    }

    /**
     * 현재 로그인한 사용자의 닉네임을 반환
     *
     * @return 로그인한 사용자의 닉네임
     * @throws IllegalStateException 인증되지 않은 경우
     */
    public static String getCurrentUserNickname() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("인증되지 않은 사용자입니다.");
        }

        Object principal = authentication.getPrincipal();

        if ("anonymousUser".equals(principal)) {
            throw new IllegalStateException("인증되지 않은 사용자입니다.");
        }

        if (principal instanceof EpikUserDetails) {
            EpikUserDetails userDetails = (EpikUserDetails) principal;
            return userDetails.getNickname();
        }

        throw new IllegalStateException("사용자 정보를 찾을 수 없습니다.");
    }

    /**
     * 현재 로그인한 사용자의 전체 정보를 반환
     *
     * @return EpikUserDetails 객체
     * @throws IllegalStateException 인증되지 않은 경우
     */
    public static EpikUserDetails getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("인증되지 않은 사용자입니다.");
        }

        Object principal = authentication.getPrincipal();

        if ("anonymousUser".equals(principal)) {
            throw new IllegalStateException("인증되지 않은 사용자입니다.");
        }

        if (principal instanceof EpikUserDetails) {
            return (EpikUserDetails) principal;
        }

        throw new IllegalStateException("사용자 정보를 찾을 수 없습니다.");
    }

    /**
     * 현재 사용자가 로그인한 상태인지 확인
     *
     * @return 로그인 여부
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Object principal = authentication.getPrincipal();

        return !(principal instanceof String && "anonymousUser".equals(principal));
    }
}

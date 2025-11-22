package com.everyplaceinkorea.epik_boot3_api.member.mypage.Info.controller;

import com.everyplaceinkorea.epik_boot3_api.auth.util.JwtUtil;
import com.everyplaceinkorea.epik_boot3_api.member.mypage.Info.dto.InfoRequestDto;
import com.everyplaceinkorea.epik_boot3_api.member.mypage.Info.service.InfoService;
import com.everyplaceinkorea.epik_boot3_api.repository.Member.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("mypage")
@RequiredArgsConstructor
public class InfoController {

    @Autowired
    public InfoService infoService;

    private AuthenticationManager authenticationManager;
    private JwtUtil jwtUtil;
    private MemberRepository memberRepository;

    public InfoController(AuthenticationManager authenticationManager, JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/info")
    public ResponseEntity<?> update(@RequestBody InfoRequestDto infoRequestDto, HttpServletResponse response, HttpServletRequest request) {

        log.debug("회원 정보 업데이트 요청 - 회원 ID: {}", infoRequestDto.getId());

        try {
            Map<String, Object> result = infoService.updateInfoWithToken(infoRequestDto, response);
            log.info("회원 정보 업데이트 성공 - 회원 ID: {}", infoRequestDto.getId());

            return ResponseEntity.ok(result);

        } catch(IllegalStateException e) {
            log.warn("권한 없음 - 회원 ID: {}, 사유: {}", infoRequestDto.getId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());

        } catch(IllegalArgumentException e) {
            log.warn("회원 정보 업데이트 실패 - 회원 ID: {}, 사유: {}", infoRequestDto.getId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());

        } catch(Exception e) {
            log.error("회원 정보 업데이트 오류 - 회원 ID: {}", infoRequestDto.getId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("회원 정보 업데이트 중 오류가 발생했습니다.");
        }
    }

    @PostMapping("/profile-image")
    public ResponseEntity<?> updateProfileImage(
            @RequestParam("profileImage")MultipartFile profileImage,
            HttpServletResponse response) {

        try {
            // 기본 검증
            if(profileImage.isEmpty()) {
                log.warn("프로필 이미지 업로드 실패 - 빈 파일");
                return ResponseEntity.badRequest().body("파일이 비어있습니다.");
            }

            // 상세 검증
            Map<String, Object> result = infoService.updateProfileImageWithToken(profileImage, response);
            log.info("프로필 이미지 업로드 성공 - 파일명: {}, 크기: {}bytes", profileImage.getOriginalFilename(), profileImage.getSize());

            return ResponseEntity.ok(result);

        } catch(IllegalArgumentException e) {
            log.warn("프로필 이미지 검증 실패 - 파일명: {}, 사유: {}", profileImage.getOriginalFilename(), e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch(IOException e) {
            log.error("프로필 이미지 저장 실패 - 파일명: {}", profileImage.getOriginalFilename(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("파일 저장 중 오류가 발생했습니다.");

        } catch(Exception e) {
            log.error("프로필 이미지 업로드 중 예상치 못한 오류 - 파일명: {}", profileImage.getOriginalFilename(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("프로필 이미지 업로드 중 오류가 발생했습니다.");
        }
    }

}

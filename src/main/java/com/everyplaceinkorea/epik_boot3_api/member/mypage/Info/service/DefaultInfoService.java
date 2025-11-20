package com.everyplaceinkorea.epik_boot3_api.member.mypage.Info.service;

import com.everyplaceinkorea.epik_boot3_api.admin.member.service.MemberService;
import com.everyplaceinkorea.epik_boot3_api.auth.entity.EpikUserDetails;
import com.everyplaceinkorea.epik_boot3_api.auth.util.JwtUtil;
import com.everyplaceinkorea.epik_boot3_api.entity.member.Member;
import com.everyplaceinkorea.epik_boot3_api.member.mypage.Info.dto.InfoRequestDto;
import com.everyplaceinkorea.epik_boot3_api.member.mypage.Info.dto.ProfilePicRequestDto;
import com.everyplaceinkorea.epik_boot3_api.member.mypage.Info.dto.ProfilePicResponseDto;
import com.everyplaceinkorea.epik_boot3_api.repository.Member.MemberRepository;
import com.everyplaceinkorea.epik_boot3_api.util.SecurityUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Slf4j
@Service
@Transactional
public class DefaultInfoService implements InfoService {

    private final MemberRepository memberRepository;
    private JwtUtil jwtUtil;

    public DefaultInfoService(MemberRepository memberRepository, JwtUtil jwtUtil) {
        this.memberRepository = memberRepository;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public UserDetails updateInfo(InfoRequestDto infoRequestDto) {

        // 1. 현재 로그인한 사용자 확인
        Long currentMemberId = SecurityUtil.getCurrentMemberId();

        // 2. 권한 검증: 요청한 ID와 로그인한 사용자 ID가 일치하는지 확인
        if (!currentMemberId.equals(infoRequestDto.getId())) {
            throw new IllegalStateException("본인의 정보만 수정할 수 있습니다.");
        }

        log.info("회원 정보 업데이트 - ID: {}", infoRequestDto.getId());

        // 3. 회원 조회
        Member existingMember = memberRepository.findById(currentMemberId)
                .orElseThrow(() -> new IllegalArgumentException("업데이트할 멤버 없음: " + currentMemberId));

        // 4. 정보 업데이트
        existingMember.setEmail(infoRequestDto.getEmail());
        existingMember.setNickname(infoRequestDto.getNickname());
        memberRepository.save(existingMember);

        log.info("회원 정보 업데이트 완료 - role: {}", existingMember.getRole());

        // 5. UserDetails 생성 및 반환
        return createUserDetails(existingMember);
    }

    @Override
    public ProfilePicResponseDto updateProfilePic(ProfilePicRequestDto profilePicRequestDto, MultipartFile profileImage) throws IOException {
        Long memberId = profilePicRequestDto.getId();
        log.info("프로필 이미지 업데이트 시작 - 회원 ID: {}", memberId);

        // 1. 파일 저장
        String savedFilePath = saveProfileImage(memberId, profileImage);

        // 2. 회원 정보 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다: " + memberId));

        // 3. DB 업데이트
        member.setProfileImg(savedFilePath);
        memberRepository.save(member);
        log.info("프로필 이미지 업데이트 완료 - 경로: {}", savedFilePath);

        // 4. 응답 DTO 생성
        return ProfilePicResponseDto.builder()
                .id(member.getId())
                .profileImg(savedFilePath)
                .build();
    }

    @Override
    public Map<String, Object> updateProfileImageWithToken(MultipartFile profileImage, HttpServletResponse response) throws IOException {

        // 1. 현재 로그인한 사용자 확인
        Long currentMemberId = SecurityUtil.getCurrentMemberId();
        log.info("프로필 이미지 및 토큰 업데이트 시작 - 인증된 회원 ID: {}", currentMemberId);

        // 2. 파일 저장
        String savedFilePath = saveProfileImage(currentMemberId, profileImage);

        // 3. 업데이트된 회원 정보 조회
        Member member = memberRepository.findById(currentMemberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        member.setProfileImg(savedFilePath);
        memberRepository.save(member);

        // 4. UserDetails 생성
        EpikUserDetails userDetails = createUserDetails(member);

        // 5. 새 JWT 토큰 생성
        String newToken = jwtUtil.generateToken(userDetails);

        // 6. 쿠키에 토큰 저장
        setTokenCookie(response, newToken);

        // 7. 응답 데이터 구성
        Map<String, Object> result = new HashMap<>();
        result.put("token", newToken);
        result.put("profileImg", savedFilePath);
        result.put("memberId", currentMemberId);

        log.info("프로필 이미지 및 토큰 업데이트 완료");
        return result;
    }

    /**
     * 프로필 이미지 파일을 서버에 저장
     */
    private String saveProfileImage(Long memberId, MultipartFile profileImage) throws IOException {
        // 업로드 디렉토리 설정
        String uploadDir = System.getProperty("user.dir") + "/uploads/images/user/";
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (!created) {
                throw new IOException("업로드 디렉토리 생성 실패: " + uploadDir);
            }
        }

        // 파일명 생성 (중복 방지)
        String originalFilename = profileImage.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String newFilename = String.format("profile_%d_%d%s",
                memberId,
                System.currentTimeMillis(),
                extension
        );

        // 파일 저장
        File destinationFile = new File(uploadDir + newFilename);
        profileImage.transferTo(destinationFile);

        log.info("프로필 이미지 저장 완료: {}", destinationFile.getAbsolutePath());

        // 상대 경로 반환 (DB에 저장될 값)
        return "uploads/images/user/" + newFilename;
    }

    /**
     * Member 엔티티로부터 EpikUserDetails 생성
     */
    private EpikUserDetails createUserDetails(Member member) {
        List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority(member.getRole())
        );

        return EpikUserDetails.builder()
                .id(member.getId())
                .username(member.getUsername())
                .password(member.getPassword())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .profileImage(member.getProfileImg())
                .authorities(authorities)
                .build();
    }

    /**
     * HTTP 응답에 JWT 토큰 쿠키 설정
     */
    private void setTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("jwt_token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60 * 24); // 1일
        response.addCookie(cookie);

        log.debug("JWT 토큰 쿠키 설정 완료");
    }


}

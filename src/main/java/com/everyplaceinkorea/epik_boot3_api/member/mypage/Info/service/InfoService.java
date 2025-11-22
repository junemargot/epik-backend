package com.everyplaceinkorea.epik_boot3_api.member.mypage.Info.service;

import com.everyplaceinkorea.epik_boot3_api.member.mypage.Info.dto.InfoRequestDto;
import com.everyplaceinkorea.epik_boot3_api.member.mypage.Info.dto.InfoResponseDto;
import com.everyplaceinkorea.epik_boot3_api.member.mypage.Info.dto.ProfilePicRequestDto;
import com.everyplaceinkorea.epik_boot3_api.member.mypage.Info.dto.ProfilePicResponseDto;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public interface InfoService {

    /**
     * 프로필 이미지 업로드 및 저장
     * @param profilePicRequestDto 요청 DTO
     * @param profileImage 업로드할 이미지 파일
     * @return 저장된 이미지 정보
     * @throws IOException
     */
    ProfilePicResponseDto updateProfilePic(ProfilePicRequestDto profilePicRequestDto, MultipartFile profileImage) throws IOException;

//    UserDetails updateInfo(InfoRequestDto infoRequestDto);

    Map<String, Object> updateInfoWithToken(InfoRequestDto infoRequestDto, HttpServletResponse response);

    /**
     * 프로필 이미지 업데이트 및 새 토큰 발급
     * @param profileImage 업로드할 이미지 파일
     * @param response HTTP 응답 (쿠키 설정용)
     * @return 토큰과 프로필 이미지 정보
     * @throws IOException
     */
    Map<String, Object> updateProfileImageWithToken(MultipartFile profileImage, HttpServletResponse response) throws IOException;
}

package com.everyplaceinkorea.epik_boot3_api.anonymous.signup.service;

import com.everyplaceinkorea.epik_boot3_api.anonymous.signup.dto.EmailCheckDto;
import com.everyplaceinkorea.epik_boot3_api.anonymous.signup.dto.NicknameCheckDto;
import com.everyplaceinkorea.epik_boot3_api.anonymous.signup.dto.SignupRequestDto;
import com.everyplaceinkorea.epik_boot3_api.anonymous.signup.dto.UsernameCheckDto;
import org.springframework.stereotype.Service;

import java.util.Map;


@Service
public interface SignupService {
    // 1. username 중복 확인
    UsernameCheckDto usernameCheck(UsernameCheckDto usernameCheckDto);

    //2. nickname 중복확인
    NicknameCheckDto nicknameCheck(NicknameCheckDto nicknameCheckDto);

    //3, 이메일 인증 확인
    Map<String, String> emailCheck(EmailCheckDto emailCheckDto);

    SignupRequestDto signup(SignupRequestDto signupRequestDto);

    // 이메일 인증 코드 확인
    Map<String, String> verifyEmailCode(String email, String code);




}

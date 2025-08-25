package com.everyplaceinkorea.epik_boot3_api.anonymous.signup.controller;

import com.everyplaceinkorea.epik_boot3_api.anonymous.signup.dto.EmailCheckDto;
import com.everyplaceinkorea.epik_boot3_api.anonymous.signup.dto.NicknameCheckDto;
import com.everyplaceinkorea.epik_boot3_api.anonymous.signup.dto.SignupRequestDto;
import com.everyplaceinkorea.epik_boot3_api.anonymous.signup.dto.UsernameCheckDto;
import com.everyplaceinkorea.epik_boot3_api.anonymous.signup.service.SignupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("signup")
public class SignupController {

    @Autowired
    public SignupService signupService;

    public SignupController(SignupService signupService) {}

    //1. username 중복확인
    @PostMapping("/checkUsername")
    public ResponseEntity<UsernameCheckDto> checkUsername(@RequestBody UsernameCheckDto usernameCheckDto){

        return ResponseEntity.ok(signupService.usernameCheck(usernameCheckDto));
    }

    //2. nickname 중복확인
    @PostMapping("/checkNickname")
    public ResponseEntity<NicknameCheckDto> checkUsername(@RequestBody NicknameCheckDto nicknameCheckDto){

        return ResponseEntity.ok(signupService.nicknameCheck(nicknameCheckDto));
    }

    //3. email 인증 확인
    @PostMapping("/checkEmail")
    public Map<String, String> sendemail(@RequestBody EmailCheckDto emailCheckDto){

        return signupService.emailCheck(emailCheckDto);
    }

    //4. 최종 버튼
    @PostMapping()
    public ResponseEntity<SignupRequestDto> signup(@RequestBody SignupRequestDto signupRequestDto) {

        SignupRequestDto saved = signupService.signup(signupRequestDto);

        return ResponseEntity.ok(saved);
    }






}

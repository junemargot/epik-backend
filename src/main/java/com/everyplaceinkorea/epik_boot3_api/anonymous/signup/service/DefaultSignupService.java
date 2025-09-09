package com.everyplaceinkorea.epik_boot3_api.anonymous.signup.service;

import com.everyplaceinkorea.epik_boot3_api.anonymous.signup.dto.*;
import com.everyplaceinkorea.epik_boot3_api.entity.member.LoginType;
import com.everyplaceinkorea.epik_boot3_api.entity.member.Member;
import com.everyplaceinkorea.epik_boot3_api.repository.Member.MemberRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class DefaultSignupService implements SignupService {

    private final ModelMapper modelMapper;
    private final MemberRepository memberRepository;

    @Autowired
    private JavaMailSender mailSender;

    // 이메일 인증 정보를 메모리에 임시 저장 (운영환경에서는 Redis 사용 권장)
    private final Map<String, EmailVerificationDto> verificationStore = new ConcurrentHashMap<>();

    public DefaultSignupService(ModelMapper modelMapper, MemberRepository memberRepository) {
        this.modelMapper = modelMapper;
        this.memberRepository = memberRepository;
    }

    // 1. username 중복 확인
    @Override
    public UsernameCheckDto usernameCheck(UsernameCheckDto usernameCheckDto) {
        UsernameCheckDto dto = new UsernameCheckDto();
        String username = usernameCheckDto.getUsername();
        Optional<Member> userCheck = memberRepository.findByUsername(username);

        if(userCheck.isPresent()) {
            dto.setUsername(username);
        } else {
            dto.setUsername(null);
        }
        return dto;
    }

    //2. nickname 중복확인
    @Override
    public NicknameCheckDto nicknameCheck(NicknameCheckDto nicknameCheckDto) {
        NicknameCheckDto dto = new NicknameCheckDto();
        String nickname = nicknameCheckDto.getNickname();
        Optional<Member> nicknameCheck = memberRepository.findByNickname(nickname);

        if(nicknameCheck.isPresent()) {
            dto.setNickname(nickname);
        } else {
            dto.setNickname(null);
        }
        return dto;
    }

    //3. 인증이메일전송
    @Override
    public Map<String, String> emailCheck(EmailCheckDto emailCheckDto) {
        Map<String, String> response = new HashMap<>();
        String email = emailCheckDto.getEmail();

        // 1. 이메일 중복 확인
        Optional<Member> existingMember = memberRepository.findByEmail(email);
        log.info("이메일 중복 확인: {}", email);

        if (existingMember.isPresent()) {
            response.put("message", "error");
            response.put("errorCode", "EMAIL_ALREADY_EXISTS");
            return response;
        }

        // 2. 기존 인증 정보가 있으면 제거
        verificationStore.remove(email);

        // 3. 인증번호 생성
        String verificationCode = generateVerificationCode();

        // 4. 이메일 발송
        boolean emailSent = sendVerificationEmail(email, verificationCode);
        if (emailSent) {
            // 5. 인증 정보 저장
            EmailVerificationDto verification = new EmailVerificationDto(email, verificationCode);
            verificationStore.put(email, verification);

            response.put("message", "success");
            response.put("verificationCode", verificationCode);
            response.put("expiresAt", verification.getExpiresAt().toString());
        } else {
            response.put("message", "error");
            response.put("errorCode", "EMAIL_SEND_FAILED");
        }

        return response;
    }

    @Override
    public Map<String, String> verifyEmailCode(String email, String code) {
        Map<String, String> response = new HashMap<>();
        EmailVerificationDto verification = verificationStore.get(email);

        if(verification == null) {
            response.put("message", "error");
            response.put("errorCode", "EMAIL_VERIFICATION_REQUEST");
            return response;
        }

        if(verification.isExpired()) {
            verificationStore.remove(email); // 만료된 인증 정보 제거
            response.put("message", "error");
            response.put("errorCode", "EMAIL_VERIFICATION_EXPIRED");
        }

        if(verification.isUsed()) {
            response.put("message", "error");
            response.put("errorCode", "EMAIL_VERIFICATION_ALREADY_USED");
            return response;
        }

        if(!verification.getVerificationCode().equals(code)) {
            response.put("message", "error");
            response.put("errorCode", "EMAIL_VERIFICATION_INVALID_CODE");
            return response;
        }

        // 인증 성공
        verification.setUsed(true);
        response.put("message", "success");
        response.put("verifiedAt", LocalDateTime.now().toString());

        return response;
    }

    // 인증번호 생성 (예: 6자리 랜덤 코드)
    private String generateVerificationCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();

        // 알파벳 3자리 + 숫자 6자리
        for (int i = 0; i < 3; i++) {
            int index = random.nextInt(26) + 65;  // 알파벳 대문자
            code.append((char) index);
        }

        for (int i = 0; i < 6; i++) {
            int num = random.nextInt(10);  // 숫자 0-9
            code.append(num);
        }

        return code.toString();
    }

    // 이메일 발송 메서드
    private boolean sendVerificationEmail(String email, String verificationCode) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();

            message.setTo(email);  // 수신 이메일
            message.setSubject("EPIK 회원가입을 위한 이메일 인증번호");
            message.setText("회원가입을 위한 인증번호는 " + verificationCode + "입니다. (5분 내 입력해주세요)");
            mailSender.send(message);  // 메일 발송
            return true;
        } catch(Exception e) {
            log.error("이메일 발송 실패: {}", e.getMessage());
            return false;
        }

    }

    //4. 회원가입 최종 버튼
    @Override
    public SignupRequestDto signup(SignupRequestDto signupRequestDto) {
        Member member = modelMapper.map(signupRequestDto, Member.class);
        member.setRole("ROLE_MEMBER");
        member.setJoinDate(LocalDate.now());
        member.setType((byte) 1);
        member.setProfileImg("basic.png");
        member.setLoginType(LoginType.ID);

        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encodedPassword = passwordEncoder.encode(member.getPassword());

        member.setPassword(encodedPassword);
        Member saved = memberRepository.save(member);

        return modelMapper.map(saved, SignupRequestDto.class);
    }
}

package com.everyplaceinkorea.epik_boot3_api.auth.service;

import com.everyplaceinkorea.epik_boot3_api.auth.entity.EpikUserDetails;
import com.everyplaceinkorea.epik_boot3_api.auth.oauth2.dto.OAuth2UserAttributes;
import com.everyplaceinkorea.epik_boot3_api.entity.member.LoginType;
import com.everyplaceinkorea.epik_boot3_api.entity.member.Member;
import com.everyplaceinkorea.epik_boot3_api.repository.Member.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
  /*
  * 사용자 정보를 처리하기 위한 클래스
  * */

  private final MemberRepository memberRepository;

  @Override
  public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
    OAuth2User oAuth2User = super.loadUser(userRequest);

    // 소셜 로그인 제공자 ID (kakao, google, naver)
    String registrationId = userRequest.getClientRegistration().getRegistrationId();

    log.info("OAuth2 로그인 시도 - 제공자: {}", registrationId);
    log.info("OAuth2 속성: {}", oAuth2User.getAttributes());

    // OAuth2 로그인 과정에서 얻은 사용자 속성을 OAuth2UserAttributes로 변환
    OAuth2UserAttributes attributes = OAuth2UserAttributes.of(
            registrationId,
            userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName(),
            oAuth2User.getAttributes()
    );

    // 사용자 정보 저장 또는 업데이트 - DB 저장
    Member member = saveOrUpdate(attributes, registrationId);

    EpikUserDetails userDetails = EpikUserDetails.builder()
            .id(member.getId())
            .username(member.getUsername())
            .email(member.getEmail())
            .nickname(member.getNickname())
            .profileImage(member.getProfileImg())
            .authorities(Collections.singletonList(new SimpleGrantedAuthority(member.getRole())))
            .attributes(oAuth2User.getAttributes()) // 원본 속성 유지
            .name(member.getNickname()) // getname 메서드용
            .build();

    log.info("OAuth2 인증 완료: 사용자 ID={}, 이메일={}", userDetails.getId(), userDetails.getEmail());

    return userDetails;
  }

  // 사용자 정보 저장 또는 업데이트
  private Member saveOrUpdate(OAuth2UserAttributes attributes, String registrationId) {
    LoginType loginType;
    if("google".equalsIgnoreCase(registrationId)) {
      loginType = LoginType.GOOGLE;
    } else if("kakao".equalsIgnoreCase(registrationId)) {
      loginType = LoginType.KAKAO;
    } else if("naver".equalsIgnoreCase(registrationId)) {
      loginType = LoginType.NAVER;
    } else {
      throw new IllegalArgumentException("Unsupported registrationId: " + registrationId);
    }

    String email = attributes.getEmail();

    Optional<Member> memberOptional = memberRepository.findByEmail(email);

    if(memberOptional.isPresent()) {
      Member member = memberOptional.get();

      if(attributes.getProfileImage() != null &&
        !attributes.getProfileImage().isEmpty() &&
        !attributes.getProfileImage().equals("basic.png")) {
        member.setProfileImg(attributes.getProfileImage());
        log.info("프로필 이미지 업데이트: {}", attributes.getProfileImage());
      }

      return memberRepository.save(member);
    } else {
      Member member = new Member();
      member.setUsername(email);
      member.setEmail(email);
      member.setNickname(attributes.getName());
      String profileImagePath = "basic.png";
      if(attributes.getProfileImage() != null && !attributes.getProfileImage().isEmpty()) {
        profileImagePath = attributes.getProfileImage();
        log.info("신규 회원 프로필 이미지: {}", profileImagePath);
      }
      member.setProfileImg(profileImagePath);
      member.setJoinDate(LocalDate.now());
      member.setType((byte) 1);
      member.setRole("ROLE_MEMBER");
      member.setLoginType(loginType);

      return memberRepository.save(member);
    }
  }
}

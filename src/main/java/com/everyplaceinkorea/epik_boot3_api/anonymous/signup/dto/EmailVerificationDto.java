package com.everyplaceinkorea.epik_boot3_api.anonymous.signup.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationDto {
  private String email;
  private String verificationCode;
  private LocalDateTime sentAt;
  private LocalDateTime expiresAt;
  private boolean isUsed;

  public EmailVerificationDto(String email, String verificationCode) {
    this.email = email;
    this.verificationCode = verificationCode;
    this.sentAt = LocalDateTime.now();
    this.expiresAt = LocalDateTime.now().plusMinutes(5); // 5분 후 만료
    this.isUsed = false;
  }

  public boolean isExpired() {
    return LocalDateTime.now().isAfter(expiresAt);
  }

  public boolean isValid() {
    return !isUsed && !isExpired();
  }
}

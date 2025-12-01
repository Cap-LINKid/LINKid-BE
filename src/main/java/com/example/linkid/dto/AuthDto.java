package com.example.linkid.dto;

import com.example.linkid.domain.Child.Gender;
import lombok.Data;

import java.time.LocalDate;

public class AuthDto {

    // 1. 아이디 중복 확인 응답
    @Data
    public static class CheckIdResponse {
        private boolean isAvailable;
        public CheckIdResponse(boolean isAvailable) { this.isAvailable = isAvailable; }
    }

    // 2. 회원가입 요청
    @Data
    public static class RegisterRequest {
        private UserRequest user;
        private ChildRequest child;
    }

    @Data
    public static class UserRequest {
        private String loginId;
        private String password;
        private String name;
    }

    @Data
    public static class ChildRequest {
        private String name;
        private LocalDate birthdate; // "yyyy-MM-dd" 자동 매핑
        private Gender gender;
    }

    // 3. 로그인 요청
    @Data
    public static class LoginRequest {
        private String loginId;
        private String password;
    }

    // 4. 토큰 응답 (공통)
    @Data
    public static class TokenResponse {
        private String accessToken;
        private String refreshToken;

        public TokenResponse(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }
    }
}
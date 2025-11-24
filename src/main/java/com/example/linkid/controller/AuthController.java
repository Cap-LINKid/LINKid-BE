package com.example.linkid.controller;

import com.example.linkid.dto.ApiResponse;
import com.example.linkid.dto.AuthDto;
import com.example.linkid.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 아이디 중복 확인
    @GetMapping("/check-name")
    public ResponseEntity<ApiResponse<AuthDto.CheckNameResponse>> checkName(@RequestParam String name) {
        boolean isAvailable = authService.checkNameAvailability(name);
        String message = isAvailable ? "사용 가능한 아이디입니다." : "이미 사용 중인 아이디입니다.";

        return ResponseEntity.ok(ApiResponse.success(new AuthDto.CheckNameResponse(isAvailable), message));
    }

    // 회원가입
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthDto.TokenResponse>> register(@RequestBody AuthDto.RegisterRequest request) {
        AuthDto.TokenResponse token = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success(token, "회원가입이 완료되었습니다."));
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDto.TokenResponse>> login(@RequestBody AuthDto.LoginRequest request) {
        AuthDto.TokenResponse token = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(token, "로그인에 성공하였습니다."));
    }
}
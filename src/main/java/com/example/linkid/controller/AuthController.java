package com.example.linkid.controller;

import com.example.linkid.dto.ApiResponse;
import com.example.linkid.dto.AuthDto;
import com.example.linkid.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "회원가입, 로그인, 중복확인")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 아이디 중복 확인
    @Operation(summary = "아이디 중복 확인", description = "회원가입 시 아이디 중복 여부를 확인합니다.")
    @GetMapping("/check-name")
    public ResponseEntity<ApiResponse<AuthDto.CheckIdResponse>> checkName(@RequestParam String loginId) {
        boolean isAvailable = authService.checkIdAvailability(loginId);
        String message = isAvailable ? "사용 가능한 아이디입니다." : "이미 사용 중인 아이디입니다.";

        return ResponseEntity.ok(ApiResponse.success(new AuthDto.CheckIdResponse(isAvailable), message));
    }

    // 회원가입
    @Operation(summary = "회원가입", description = "부모와 자녀 정보를 입력받아 회원가입을 진행하고, 자동 로그인 처리합니다.")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthDto.TokenResponse>> register(@RequestBody AuthDto.RegisterRequest request) {
        AuthDto.TokenResponse token = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success(token, "회원가입이 완료되었습니다."));
    }

    // 로그인
    @Operation(summary = "로그인", description = "아이디와 비밀번호로 로그인하여 토큰을 발급받습니다.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDto.TokenResponse>> login(@RequestBody AuthDto.LoginRequest request) {
        AuthDto.TokenResponse token = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(token, "로그인에 성공하였습니다."));
    }
}
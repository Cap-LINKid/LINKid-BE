package com.example.linkid.controller;

import com.example.linkid.dto.ApiResponse;
import com.example.linkid.dto.MyPageDto;
import com.example.linkid.service.MyPageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "MyPage", description = "마이페이지")
@RestController
@RequestMapping("/api/v1/my-page")
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageService myPageService;

    @Operation(summary = "마이페이지 정보 조회", description = "유저 정보, 자녀 정보, 활동 요약 통계를 조회합니다.")
    @GetMapping("")
    public ResponseEntity<ApiResponse<MyPageDto.MyPageResponse>> getMyPageInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        MyPageDto.MyPageResponse response = myPageService.getMyPageInfo(username);

        return ResponseEntity.ok(ApiResponse.success(response, "마이페이지 정보를 조회했습니다."));
    }
}
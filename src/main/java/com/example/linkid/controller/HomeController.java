package com.example.linkid.controller;

import com.example.linkid.dto.ApiResponse;
import com.example.linkid.dto.HomeDto;
import com.example.linkid.service.HomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Home", description = "홈 화면")
@RestController
@RequestMapping("/api/v1/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @Operation(summary = "홈 화면 데이터 조회", description = "성장 리포트 그래프 데이터와 이번 주 핵심 챌린지를 조회합니다.")
    @GetMapping("")
    public ResponseEntity<ApiResponse<HomeDto.HomeResponse>> getHomeData() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        HomeDto.HomeResponse homeData = homeService.getHomeData(username);

        return ResponseEntity.ok(ApiResponse.success(homeData, "홈 정보를 조회했습니다."));
    }
}
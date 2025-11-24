package com.example.linkid.controller;

import com.example.linkid.dto.ApiResponse;
import com.example.linkid.dto.HomeDto;
import com.example.linkid.service.HomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @GetMapping("")
    public ResponseEntity<ApiResponse<HomeDto.HomeResponse>> getHomeData() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        HomeDto.HomeResponse homeData = homeService.getHomeData(username);

        return ResponseEntity.ok(ApiResponse.success(homeData, "홈 정보를 조회했습니다."));
    }
}
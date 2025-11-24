package com.example.linkid.controller;

import com.example.linkid.dto.ApiResponse;
import com.example.linkid.dto.ReportDto;
import com.example.linkid.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // 목록 조회
    @GetMapping("")
    public ResponseEntity<ApiResponse<List<ReportDto.ReportListResponse>>> getReportList() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        List<ReportDto.ReportListResponse> response = reportService.getReportList(username);

        return ResponseEntity.ok(ApiResponse.success(response, "분석 리포트 목록을 조회했습니다."));
    }

    // 상세 조회
    @GetMapping("/{reportId}")
    public ResponseEntity<ApiResponse<ReportDto.ReportDetailResponse>> getReportDetail(@PathVariable Long reportId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        ReportDto.ReportDetailResponse response = reportService.getReportDetail(reportId, username);

        return ResponseEntity.ok(ApiResponse.success(response, "상세 리포트를 조회했습니다."));
    }
}
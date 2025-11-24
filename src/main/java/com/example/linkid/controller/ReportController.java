package com.example.linkid.controller;

import com.example.linkid.dto.ApiResponse;
import com.example.linkid.dto.ReportDto;
import com.example.linkid.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Report", description = "분석 리포트 조회")
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // 목록 조회
    @Operation(summary = "리포트 목록 조회", description = "해당 자녀의 모든 분석 리포트 목록을 최신순으로 조회합니다.")
    @GetMapping("")
    public ResponseEntity<ApiResponse<List<ReportDto.ReportListResponse>>> getReportList() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        List<ReportDto.ReportListResponse> response = reportService.getReportList(username);

        return ResponseEntity.ok(ApiResponse.success(response, "분석 리포트 목록을 조회했습니다."));
    }

    // 상세 조회
    @Operation(summary = "리포트 상세 조회", description = "특정 리포트의 상세 분석 결과(AI 결과 포함)를 조회합니다.")
    @GetMapping("/{reportId}")
    public ResponseEntity<ApiResponse<ReportDto.ReportDetailResponse>> getReportDetail(@PathVariable Long reportId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        ReportDto.ReportDetailResponse response = reportService.getReportDetail(reportId, username);

        return ResponseEntity.ok(ApiResponse.success(response, "상세 리포트를 조회했습니다."));
    }
}
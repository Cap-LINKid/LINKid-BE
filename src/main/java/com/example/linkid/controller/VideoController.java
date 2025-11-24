package com.example.linkid.controller;

import com.example.linkid.service.VideoAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoAnalysisService videoAnalysisService;

    // 1. 영상 업로드용 Presigned URL 요청
    @PostMapping("/presign")
    public ResponseEntity<?> getPresignedUrl(@RequestBody Map<String, String> request) {
        // TODO: SecurityContextHolder에서 userId 추출 필요
        Long userId = 1L;

        Map<String, Object> data = videoAnalysisService.generatePresignedUrl(
                userId,
                request.get("fileName"),
                request.get("contentType"),
                request.get("contextTag")
        );

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", data,
                "message", "업로드 URL이 발급되었습니다."
        ));
    }

    // 2. 영상 분석 시작 요청
    @PostMapping("/{videoId}/start")
    public ResponseEntity<?> startAnalysis(@PathVariable Long videoId) {
        videoAnalysisService.startAnalysis(videoId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                        "videoId", videoId,
                        "status", "STT_PROCESSING",
                        "message", "음성을 텍스트로 변환하는 중입니다"
                ),
                "message", "영상 분석을 시작합니다."
        ));
    }

    // 3. 분석 상태 조회 (Polling)
    @GetMapping("/{videoId}/status")
    public ResponseEntity<?> getStatus(@PathVariable Long videoId) {
        Map<String, Object> statusData = videoAnalysisService.checkStatus(videoId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", statusData,
                "message", "분석 상태를 조회했습니다."
        ));
    }
}
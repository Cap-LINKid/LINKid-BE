package com.example.linkid.controller;

import com.example.linkid.domain.User;
import com.example.linkid.repository.UserRepository;
import com.example.linkid.service.VideoAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Video", description = "영상 업로드 및 분석")
@RestController
@RequestMapping("/api/v1/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoAnalysisService videoAnalysisService;
    private final UserRepository userRepository;

    // 1. 영상 업로드용 Presigned URL 요청
    @Operation(summary = "업로드 URL 발급", description = "영상 파일명과 길이(초)를 받아 S3 Presigned URL을 발급합니다.")
    @PostMapping("/presign")
    public ResponseEntity<?> getPresignedUrl(@RequestBody Map<String, Object> request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        User user = userRepository.findByLoginId(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("로그인된 사용자 정보를 찾을 수 없습니다."));

        Long userId = user.getUserId();

        Integer duration = request.containsKey("duration") ? (Integer) request.get("duration") : 0;

        Map<String, Object> data = videoAnalysisService.generatePresignedUrl(
                userId,
                (String) request.get("fileName"),
                (String) request.get("contentType"),
                (String) request.get("contextTag"),
                duration
        );

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", data,
                "message", "업로드 URL이 발급되었습니다."
        ));
    }

    // 2. 영상 분석 시작 요청
    @Operation(summary = "영상 분석 시작", description = "업로드가 완료된 영상의 분석(STT 및 AI 분석)을 시작합니다.")
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
    @Operation(summary = "분석 상태 조회 (Polling)", description = "분석 진행 상태를 주기적으로 조회합니다. 완료 시 결과를 반환하지 않고 상태만 확인합니다.")
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
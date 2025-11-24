package com.example.linkid.controller;

import com.example.linkid.domain.User;
import com.example.linkid.repository.UserRepository;
import com.example.linkid.service.VideoAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoAnalysisService videoAnalysisService;
    private final UserRepository userRepository;

    // 1. 영상 업로드용 Presigned URL 요청
    @PostMapping("/presign")
    public ResponseEntity<?> getPresignedUrl(@RequestBody Map<String, Object> request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        User user = userRepository.findByName(currentUsername)
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
package com.example.linkid.controller;

import com.example.linkid.dto.ApiResponse;
import com.example.linkid.dto.ChallengeDto;
import com.example.linkid.service.ChallengeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/challenges")
@RequiredArgsConstructor
public class ChallengeController {

    private final ChallengeService challengeService;

    // 목록 조회 (?status=COMPLETED or ACTIVE)
    @GetMapping("")
    public ResponseEntity<ApiResponse<List<ChallengeDto.ChallengeListResponse>>> getList(
            @RequestParam(defaultValue = "ACTIVE") String status) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        List<ChallengeDto.ChallengeListResponse> response = challengeService.getChallengeList(username, status);
        return ResponseEntity.ok(ApiResponse.success(response, "챌린지 목록을 조회했습니다."));
    }

    // 상세 조회
    @GetMapping("/{challengeId}")
    public ResponseEntity<ApiResponse<ChallengeDto.ChallengeDetailResponse>> getDetail(@PathVariable Long challengeId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        ChallengeDto.ChallengeDetailResponse response = challengeService.getChallengeDetail(challengeId, username);
        return ResponseEntity.ok(ApiResponse.success(response, "챌린지 상세를 조회했습니다."));
    }

    // 챌린지 수락(생성)
    @PostMapping("/accept")
    public ResponseEntity<ApiResponse<Long>> acceptChallenge(@RequestBody Map<String, Long> request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Long reportId = request.get("reportId");
        Long challengeId = challengeService.createChallengeFromReport(reportId, username);
        return ResponseEntity.ok(ApiResponse.success(challengeId, "챌린지가 생성(수락)되었습니다."));
    }

    @PostMapping("/actions/{actionId}/complete")
    public ResponseEntity<ApiResponse<ChallengeDto.CompleteActionResponse>> completeAction(
            @PathVariable Long actionId,
            @RequestBody ChallengeDto.CompleteActionRequest request) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        ChallengeDto.CompleteActionResponse response = challengeService.completeAction(
                actionId,
                username,
                request.getMemo()
        );

        return ResponseEntity.ok(ApiResponse.success(response, "챌린지 행동이 완료되었습니다."));
    }
}
package com.example.linkid.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class ChallengeDto {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChallengeListResponse {
        private Long challengeId;
        private String title;
        private String period; // "MM월 dd일 ~ MM월 dd일"
        private int progressPercent;
        private String status;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChallengeDetailResponse {
        private Long challengeId;
        private String title;
        private String goal;
        private String period;
        private int progressPercent;
        private int currentCount;    // 현재 완료 갯수 (2)
        private int totalCount;      // 전체 목표 갯수 (3)

        private Long relatedReportId;

        private List<ActionItem> actions;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ActionItem {
        private Long actionId;
        private String content;
        private boolean isCompleted;
        private String completedDate; // "9월 10일" (완료된 경우만)
        private String reflection;    // "나의 회고" 내용 (있으면 표시)
    }

    @Data
    @NoArgsConstructor
    public static class CreateRequest {
        private Long reportId;
    }
}
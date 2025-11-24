package com.example.linkid.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

public class MyPageDto {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MyPageResponse {
        private String userName;
        private ChildInfo childInfo;
        private ActivitySummary activitySummary;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChildInfo {
        private Long childId;
        private String name;
        private LocalDate birthdate;
        private String gender;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ActivitySummary {
        private int totalReports;
        private int totalChallenges;
    }
}
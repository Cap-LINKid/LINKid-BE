package com.example.linkid.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import java.util.List;

public class HomeDto {

    @Data
    @Builder
    public static class HomeResponse {
        private GrowthReport growthReport;
        private ActiveChallenge activeChallenge;
    }

    @Data
    @Builder
    public static class GrowthReport {
        private List<QiScorePoint> qiScoreHistory;
        private List<PiNdiPoint> piNdiHistory;
    }

    @Data
    @AllArgsConstructor
    public static class QiScorePoint {
        private String date;  // "MM.dd"
        private int score;
    }

    @Data
    @AllArgsConstructor
    public static class PiNdiPoint {
        private String date; // "MM.dd"
        private int pi;
        private int ndi;
    }

    @Data
    @Builder
    public static class ActiveChallenge {
        private Long challengeId;
        private String title;
        private String goal;
        private String period; // "YYYY.MM.dd. - YYYY.MM.dd."
    }
}
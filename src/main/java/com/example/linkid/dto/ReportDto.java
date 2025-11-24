package com.example.linkid.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ReportDto {

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ReportListResponse {
        private Long reportId;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;

        private String contextTag;
        private Integer durationSeconds;
        private String relationshipStatus;
        private BigDecimal piScore;
        private BigDecimal ndiScore;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ReportDetailResponse {
        private Long reportId;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;

        private String username;
        private AiApiDto.AiResult content;
    }
}
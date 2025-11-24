package com.example.linkid.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

public class AiApiDto {

    @Data
    @Builder
    public static class AnalyzeRequest {
        @JsonProperty("utterances_ko")
        private List<Utterance> utterancesKo;

        @JsonProperty("challenge_specs")
        private List<ChallengeSpec> challengeSpecs;

        private MetaData meta;
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class Utterance {
        private String speaker;    // "A", "B" (화자 분리 결과)
        private String text;       // 발화 내용
        private Integer timestamp; // 시작 시간 (ms)
    }

    @Data
    @Builder
    public static class ChallengeSpec {
        @JsonProperty("challenge_id")
        private String challengeId; // AI에 보낼 때는 String으로 변환

        private String title;
        private String goal;
        private List<String> actions; // 행동 내용들 (String 리스트)
    }

    @Data
    @Builder
    public static class MetaData {
        private String childName;
        private String childGender;    // "MALE", "FEMALE"
        private String childBirthDate; // "YYYY-MM-DD"
        private String contextTag;     // "자유놀이", "식사시간" 등
        private Integer videoDuration; // 영상 길이 (초, 선택사항)
    }

    @Data
    @NoArgsConstructor
    public static class AnalyzeResponse {
        private String execution_id;
        private String status;
        private String message;
    }

    @Data
    @NoArgsConstructor
    public static class StatusResponse {
        private String execution_id;
        private String status; // pending, running, completed, failed
        private String analysis_status;
        private Integer progress_percentage;
        private String status_message;
        private JsonNode result;
    }
}
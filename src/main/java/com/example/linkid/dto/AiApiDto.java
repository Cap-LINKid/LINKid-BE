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
        private String speaker;
        private String text;
        private Integer timestamp;
    }

    @Data
    @Builder
    public static class ChallengeSpec {
        private String challenge_id;
        private String title;
        private String goal;
        private List<String> actions;
    }

    @Data
    @Builder
    public static class MetaData {
        private String childName;
        private String childGender;
        private String childBirthDate;
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
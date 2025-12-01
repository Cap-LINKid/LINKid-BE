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
        private List<ActionSpec> actions;
    }

    @Data @Builder
    public static class ActionSpec {
        @JsonProperty("action_id")
        private String actionId;
        private String content;
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
        private String status;          // "running", "completed", "failed"
        private String analysis_status; // "translating", "labeling", ... "completed"
        private Integer progress_percentage;
        private String status_message;
        private AiResult result;
    }

    @Data
    @NoArgsConstructor
    public static class AiResult {
        private SummaryDiagnosis summary_diagnosis;
        private JsonNode key_moment_capture;
        private StyleAnalysis style_analysis;
        private CoachingAndPlan coaching_and_plan; // 챌린지 정보 추출을 위해 매핑 필요
        private GrowthReport growth_report;
        private Scores scores;
    }

    @Data
    @NoArgsConstructor
    public static class StyleAnalysis {
        @JsonProperty("interaction_style")
        private InteractionStyle interactionStyle;
        private String summary;
    }

    @Data
    @NoArgsConstructor
    public static class InteractionStyle {
        @JsonProperty("parent_analysis")
        private AnalysisDetail parentAnalysis;

        @JsonProperty("child_analysis")
        private AnalysisDetail childAnalysis;
    }

    @Data
    @NoArgsConstructor
    public static class AnalysisDetail {
        private List<Category> categories;
    }

    @Data
    @NoArgsConstructor
    public static class Category {
        private String name;  // "반영적 듣기"
        private Double ratio; // 0.20
        private String label; // "RD"
    }

    @Data
    @NoArgsConstructor
    public static class GrowthReport {
        private JsonNode analysis_session;
        private List<Metric> current_metrics;

        @JsonProperty("challenge_evaluations")
        private List<ChallengeEvaluation> challengeEvaluations;
    }

    @Data
    @NoArgsConstructor
    public static class ChallengeEvaluation {
        @JsonProperty("challenge_name")
        private String challengeName;

        @JsonProperty("action_id")
        private Long actionId;

        @JsonProperty("detected_count")
        private Integer detectedCount;

        private String description;
        private JsonNode instances;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Metric {
        private String label;      // "반영적 듣기"
        private Double before;     // [추가] 이전 값 (예: 15.0)
        private Double after;      // [추가] 현재 값 (예: 35.0)
        private Double diff;       // 변화량 (+20.0)
        private String value_type; // "ratio" 고정
    }

    @Data
    @NoArgsConstructor
    public static class SummaryDiagnosis {
        private String stage_name;     // 공감적 협력
        private Double positive_ratio;
        private Double negative_ratio;
    }

    @Data
    @NoArgsConstructor
    public static class Scores {
        private Double pi_score;
        private Double ndi_score;
    }

    @Data
    @NoArgsConstructor
    public static class CoachingAndPlan {
        private CoachingPlan coaching_plan;
    }

    @Data
    @NoArgsConstructor
    public static class CoachingPlan {
        private String summary;
        private GeneratedChallenge challenge;
        private String rationale;

        @JsonProperty("qa_tips")
        private JsonNode qaTips;
    }

    @Data
    @NoArgsConstructor
    public static class GeneratedChallenge {
        private String title;
        private String goal;
        private Integer period_days;
        private SuggestedPeriod suggested_period;
        private List<String> actions;
    }

    @Data
    @NoArgsConstructor
    public static class SuggestedPeriod {
        private String start;
        private String end;
    }
}
package com.example.linkid.service;

import com.example.linkid.dto.AiApiDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiAnalysisService {

    @Value("${ai.server.url}")
    private String aiServerUrl;

    private final WebClient.Builder webClientBuilder;

    // 분석 요청 (POST /analyze)
    public String requestAnalysis(AiApiDto.AnalyzeRequest request) {
        AiApiDto.AnalyzeResponse response = webClientBuilder.build()
                .post()
                .uri(aiServerUrl + "/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AiApiDto.AnalyzeResponse.class)
                .block();

        if (response != null) {
            log.info("AI 분석 요청 성공. Execution ID: {}", response.getExecution_id());
            return response.getExecution_id();
        }
        throw new RuntimeException("AI 서버 요청 실패");
    }

    // 상태 조회 (GET /status/{executionId})
    public AiApiDto.StatusResponse getStatus(String executionId) {
        return webClientBuilder.build()
                .get()
                .uri(aiServerUrl + "/status/" + executionId)
                .retrieve()
                .bodyToMono(AiApiDto.StatusResponse.class)
                .block();
    }
}
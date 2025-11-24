package com.example.linkid.service;

import com.example.linkid.domain.*;
import com.example.linkid.dto.AiApiDto;
import com.example.linkid.repository.ChallengeRepository;
import com.example.linkid.repository.ChildRepository;
import com.example.linkid.repository.VideoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncAnalysisService {

    private final VideoRepository videoRepository;
    private final ChildRepository childRepository;
    private final ChallengeRepository challengeRepository;

    private final ClovaSpeechService clovaSpeechService;
    private final ObjectStorageService objectStorageService;
    private final AiAnalysisService aiAnalysisService;

    /**
     * 비동기 분석 파이프라인
     * STT 변환 -> (AI 분석 요청)
     */
    @Async
    @Transactional
    public void processVideoAsync(Long videoId) {
        log.info("비동기 분석 파이프라인 시작 (별도 스레드): VideoId {}", videoId);
        try {
            Video video = videoRepository.findById(videoId)
                    .orElseThrow(() -> new IllegalArgumentException("영상을 찾을 수 없습니다."));

            // 1. Clova Speech STT 요청
            // 버킷이 비공개이므로 다운로드용 Presigned URL 사용
            String objectUrl = objectStorageService.generatePresignedDownloadUrl(video.getBucketKey());
            log.info("Clova STT 요청 URL: {}", objectUrl);
            JsonNode sttResult = clovaSpeechService.recognizeSpeechFromUrl(objectUrl);

            video.setSttResult(sttResult.toString());

            // STT 결과 확인
            video.setStatus(VideoStatus.STT_COMPLETED);
            video.setStatusUpdatedAt(LocalDateTime.now());
            videoRepository.save(video);

            log.info("STT 변환 완료. VideoId: {}", videoId);

            // 2. AI 요청 데이터 구성

            // (1) Utterances (STT 결과 변환)
            List<AiApiDto.Utterance> utterances = parseSttToUtterances(sttResult);

            // (2) Meta Data (아이 정보 + 영상 태그)
            Child child = childRepository.findById(video.getChildId())
                    .orElseThrow(() -> new IllegalArgumentException("자녀 정보를 찾을 수 없습니다."));

            AiApiDto.MetaData metaData = AiApiDto.MetaData.builder()
                    .childName(child.getName())
                    .childGender(child.getGender().name())
                    .childBirthDate(child.getBirthdate().toString()) // LocalDate -> String
                    .contextTag(video.getContextTag()) // 영상 업로드 시 받은 태그
                    .build();

            // (3) Active Challenges (진행 중인 챌린지 목록)
            List<Challenge> activeChallenges = challengeRepository.findAllByChildIdAndStatus(
                    child.getChildId(),
                    ChallengeStatus.PROCEEDING
            );

            List<AiApiDto.ChallengeSpec> challengeSpecs = activeChallenges.stream()
                    .map(challenge -> AiApiDto.ChallengeSpec.builder()
                            .challengeId(String.valueOf(challenge.getChallengeId()))
                            .title(challenge.getTitle())
                            .goal(challenge.getGoal())
                            // 챌린지 하위의 행동(Action)들의 내용을 리스트로 추출
                            .actions(challenge.getActions().stream()
                                    .filter(action -> !action.isCompleted())
                                    .map(action -> AiApiDto.ActionSpec.builder()
                                            .actionId(String.valueOf(action.getActionId()))
                                            .content(action.getContent())
                                            .build())
                                    .collect(Collectors.toList()))
                            .build())
                    .collect(Collectors.toList());

            // 3. AI 분석 요청 객체 생성
            AiApiDto.AnalyzeRequest aiRequest = AiApiDto.AnalyzeRequest.builder()
                    .utterancesKo(utterances)
                    .challengeSpecs(challengeSpecs)
                    .meta(metaData)
                    .build();

            // 4. AI 서버 호출
            log.info("AI 서버로 분석 요청 전송...");
            String executionId = aiAnalysisService.requestAnalysis(aiRequest);

            // 5. 결과 업데이트 (AI 실행 ID 저장 및 상태 변경)
            video.setAiExecutionId(executionId);
            video.setStatus(VideoStatus.AI_ANALYZING);
            video.setStatusUpdatedAt(LocalDateTime.now());
            videoRepository.save(video);

            log.info("AI 분석 요청 완료. Execution ID: {}", executionId);

        } catch (Exception e) {
            log.error("비동기 분석 중 오류 발생", e);
            updateStatusToFailed(videoId);
        }
    }

    private void updateStatusToFailed(Long videoId) {
        videoRepository.findById(videoId).ifPresent(video -> {
            video.setStatus(VideoStatus.FAILED);
            video.setErrorMessage("분석 중 오류 발생: " + LocalDateTime.now());
            videoRepository.save(video);
        });
    }

    private List<AiApiDto.Utterance> parseSttToUtterances(JsonNode sttJson) {
        List<AiApiDto.Utterance> list = new ArrayList<>();
        JsonNode segments = sttJson.path("segments");
        if (segments.isArray()) {
            for (JsonNode seg : segments) {
                list.add(new AiApiDto.Utterance(
                        seg.path("speaker").path("name").asText(),
                        seg.path("text").asText(),
                        seg.path("start").asInt()
                ));
            }
        }
        return list;
    }
}
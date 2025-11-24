package com.example.linkid.service;

import com.example.linkid.domain.Video;
import com.example.linkid.domain.VideoStatus;
import com.example.linkid.dto.AiApiDto;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncAnalysisService {

    private final VideoRepository videoRepository;
    private final ClovaSpeechService clovaSpeechService;
    private final ObjectStorageService objectStorageService;
    // private final AiAnalysisService aiAnalysisService;

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

            /* AI 연결 시  ---
            video.setStatus(VideoStatus.AI_ANALYZING);
            videoRepository.save(video);

            List<AiApiDto.Utterance> utterances = parseSttToUtterances(sttResult);
            // ... AI 요청 로직 ...
            */

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
                        seg.path("timestamp").asInt()
                ));
            }
        }
        return list;
    }
}
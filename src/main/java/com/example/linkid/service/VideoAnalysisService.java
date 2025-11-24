package com.example.linkid.service;

import com.example.linkid.domain.*;
import com.example.linkid.dto.AiApiDto;
import com.example.linkid.repository.AnalysisReportRepository;
import com.example.linkid.repository.ChildRepository;
import com.example.linkid.repository.UserRepository;
import com.example.linkid.repository.VideoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoAnalysisService {

    private final VideoRepository videoRepository;
    private final AnalysisReportRepository reportRepository;
    private final ChildRepository childRepository;
    private final UserRepository userRepository;
    private final ObjectStorageService objectStorageService;
    private final AiAnalysisService aiAnalysisService;

    private final AsyncAnalysisService asyncAnalysisService;

    // 영상 업로드 Presigned URL 발급
    public Map<String, Object> generatePresignedUrl(Long userId, String fileName, String contentType, String contextTag) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        // 자녀가 1명 가정
        Child child = childRepository.findFirstByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("등록된 자녀가 없습니다."));

        String bucketKey = "user-" + userId + "/" + System.currentTimeMillis() + "-" + fileName;
        String uploadUrl = objectStorageService.generatePresignedUploadUrl(bucketKey, contentType);

        // Video 메타데이터 임시 저장
        Video video = new Video();
        video.setUserId(userId);
        video.setChildId(child.getChildId());
        video.setFileName(fileName);
        video.setBucketKey(bucketKey);
        video.setContentType(contentType);
        video.setContextTag(contextTag);

        // 원본 URL 저장 (다운로드용 URL 아님, 단순 참조용)
        String objectUrl = objectStorageService.getObjectUrl(bucketKey);
        video.setOriginalVideoUrl(objectUrl);

        video.setStatus(VideoStatus.UPLOADING);

        videoRepository.save(video);

        return Map.of(
                "videoId", video.getVideoId(),
                "uploadUrl", uploadUrl,
                "bucketKey", bucketKey
        );
    }

    // 분석 시작 (비동기 처리 호출)
    @Transactional
    public void startAnalysis(Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 영상입니다."));

        video.setStatus(VideoStatus.STT_PROCESSING);
        video.setStatusUpdatedAt(LocalDateTime.now());
        videoRepository.save(video);

        // 비동기 서비스 호출 (별도 스레드 생성됨)
        asyncAnalysisService.processVideoAsync(video.getVideoId());

        log.info("메인 스레드: 분석 요청 완료 후 즉시 응답 반환");
    }

    // 상태 조회 및 AI 완료 시 결과 저장 (Lazy Polling)
    @Transactional
    public Map<String, Object> checkStatus(Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 영상입니다."));

        // 1. STT 완료 상태 확인
        if (video.getStatus() == VideoStatus.STT_COMPLETED) {
            return Map.of(
                    "videoId", video.getVideoId(),
                    "status", "STT_COMPLETED",
                    "message", "STT 변환이 완료되었습니다. 결과를 확인하세요.",
                    "sttResult", video.getSttResult() // 결과 확인용
            );
        }

        // 2. AI 분석 중 상태 확인
        if (video.getStatus() == VideoStatus.AI_ANALYZING && video.getAiExecutionId() != null) {
            try {
                // AI 서버에 현재 상태 조회
                AiApiDto.StatusResponse aiStatus = aiAnalysisService.getStatus(video.getAiExecutionId());

                if ("completed".equalsIgnoreCase(aiStatus.getStatus())) {
                    // 분석 완료 시 리포트 저장
                    saveAnalysisResult(video, aiStatus.getResult());

                    AnalysisReport report = reportRepository.findByVideo(video)
                            .orElseThrow(() -> new IllegalStateException("리포트가 생성되지 않았습니다."));

                    return Map.of(
                            "videoId", video.getVideoId(),
                            "status", "COMPLETED",
                            "message", "분석이 완료되었습니다",
                            "reportId", report.getReportId()
                    );
                } else if ("failed".equalsIgnoreCase(aiStatus.getStatus())) {
                    video.setStatus(VideoStatus.FAILED);
                    videoRepository.save(video); // 상태 저장 필요
                } else {
                    // [진행 중] 상세 상태 반환
                    return Map.of(
                            "videoId", video.getVideoId(),
                            "status", "AI_ANALYZING",
                            "detailStatus", aiStatus.getAnalysis_status(),
                            "progress", aiStatus.getProgress_percentage(),
                            "message", aiStatus.getStatus_message() != null ? aiStatus.getStatus_message() : "AI 분석 진행 중..."
                    );
                }
            } catch (Exception e) {
                log.error("AI 상태 조회 실패", e);
            }
        }

        // 그 외 상태 반환
        return Map.of(
                "videoId", video.getVideoId(),
                "status", video.getStatus().name(),
                "message", getStatusMessage(video.getStatus())
        );
    }

    private void saveAnalysisResult(Video video, JsonNode aiResult) {
        AnalysisReport report = new AnalysisReport();
        report.setVideo(video);
        report.setUserId(video.getUserId());
        report.setChildId(video.getChildId());

        JsonNode scores = aiResult.get("scores");
        if (scores != null) {
            report.setPiScore(new BigDecimal(scores.path("pi_score").asDouble()));
            report.setNdiScore(new BigDecimal(scores.path("ndi_score").asDouble()));
        }

        JsonNode summary = aiResult.get("summary_diagnosis");
        if (summary != null) {
            report.setRelationshipStatus(summary.path("stage_name").asText());
        }

        report.setContent(aiResult.toString());
        reportRepository.save(report);

        video.setStatus(VideoStatus.COMPLETED);
        videoRepository.save(video);
    }

    private String getStatusMessage(VideoStatus status) {
        return switch (status) {
            case UPLOADING -> "영상을 업로드하는 중입니다. 분석 준비중입니다.";
            case STT_PROCESSING -> "음성을 텍스트로 변환하는 중입니다";
            case STT_COMPLETED -> "STT 변환이 완료되었습니다";
            case AI_ANALYZING -> "AI가 상호작용 패턴을 분석하는 중입니다";
            case COMPLETED -> "분석이 완료되었습니다";
            case FAILED -> "분석 중 오류가 발생했습니다";
            default -> "대기 중";
        };
    }
}
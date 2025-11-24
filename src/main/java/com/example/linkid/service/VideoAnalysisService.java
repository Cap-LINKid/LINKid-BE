package com.example.linkid.service;

import com.example.linkid.domain.*;
import com.example.linkid.dto.AiApiDto;
import com.example.linkid.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoAnalysisService {

    private final VideoRepository videoRepository;
    private final AnalysisReportRepository reportRepository;
    private final ChallengeRepository challengeRepository;
    private final ChildRepository childRepository;
    private final UserRepository userRepository;
    private final ObjectStorageService objectStorageService;
    private final AiAnalysisService aiAnalysisService;

    private final AsyncAnalysisService asyncAnalysisService;
    private final ObjectMapper objectMapper;

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
                    "message", "STT 변환이 완료되었습니다. 분석 대기 중입니다."
            );
        }

        // 2. AI 분석 중 상태 확인
        if (video.getStatus() == VideoStatus.AI_ANALYZING && video.getAiExecutionId() != null) {
            try {
                // AI 서버에 현재 상태 조회
                AiApiDto.StatusResponse aiStatus = aiAnalysisService.getStatus(video.getAiExecutionId());

                if ("completed".equalsIgnoreCase(aiStatus.getStatus())) {
                    AiApiDto.AiResult finalResult = aiStatus.getResult();
                    updateGrowthMetrics(video.getChildId(), finalResult);
                    // 분석 완료 시 리포트 저장
                    saveAnalysisResult(video, finalResult);

                    AnalysisReport report = reportRepository.findByVideo(video)
                            .orElseThrow(() -> new IllegalStateException("리포트가 생성되지 않았습니다."));

                    return Map.of(
                            "videoId", video.getVideoId(),
                            "status", "COMPLETED",
                            "message", "분석이 완료되었습니다",
                            "reportId", report.getReportId(),
                            "result", finalResult
                    );
                } else if ("failed".equalsIgnoreCase(aiStatus.getStatus())) {
                    video.setStatus(VideoStatus.FAILED);
                    video.setErrorMessage("AI 분석 실패: " + aiStatus.getStatus_message());
                    videoRepository.save(video);
                    return Map.of("status", "FAILED", "message", "분석 중 오류가 발생했습니다.");
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
                log.error("AI 상태 조회 중 통신 오류", e);
            }
        }

        // 그 외 상태 반환
        return Map.of(
                "videoId", video.getVideoId(),
                "status", video.getStatus().name(),
                "message", getStatusMessage(video.getStatus())
        );
    }

    private void updateGrowthMetrics(Long childId, AiApiDto.AiResult currentResult) {
        try {
            if (currentResult.getStyle_analysis() == null ||
                    currentResult.getStyle_analysis().getParent_analysis() == null) {
                return;
            }
            List<AiApiDto.Category> currentCategories = currentResult.getStyle_analysis().getParent_analysis().getCategories();

            // 직전 리포트 가져오기
            Optional<AnalysisReport> prevReportOpt = reportRepository.findFirstByChildIdOrderByCreatedAtDesc(childId);

            List<AiApiDto.Metric> diffMetrics = new ArrayList<>();

            if (prevReportOpt.isPresent()) {
                // 직전 결과 파싱
                AnalysisReport prevReport = prevReportOpt.get();
                AiApiDto.AiResult prevResult = objectMapper.readValue(prevReport.getContent(), AiApiDto.AiResult.class);

                if (prevResult.getStyle_analysis() != null && prevResult.getStyle_analysis().getParent_analysis() != null) {
                    List<AiApiDto.Category> prevCategories = prevResult.getStyle_analysis().getParent_analysis().getCategories();

                    // Map으로 변환하여 검색 속도 향상 (Label -> Ratio)
                    Map<String, Double> prevMap = prevCategories.stream()
                            .collect(Collectors.toMap(AiApiDto.Category::getLabel, AiApiDto.Category::getRatio));

                    // 차이 계산
                    for (AiApiDto.Category curr : currentCategories) {
                        Double prevVal = prevMap.getOrDefault(curr.getLabel(), 0.0);
                        Double currVal = curr.getRatio();
                        double diff = currVal - prevVal; // 변화량 (예: 0.2 - 0.1 = 0.1)

                        diffMetrics.add(AiApiDto.Metric.builder()
                                .key(curr.getLabel()) // 키는 라벨로 사용
                                .label(curr.getName())
                                .value(currVal)
                                .value_type("ratio")
                                .diff(diff)
                                .build());
                    }
                }
            } else {
                // 직전 리포트가 없는 경우 (첫 분석): 현재 값 기준 상위 3개 or 그대로 보여주기
                for (AiApiDto.Category curr : currentCategories) {
                    diffMetrics.add(AiApiDto.Metric.builder()
                            .key(curr.getLabel())
                            .label(curr.getName())
                            .value(curr.getRatio())
                            .value_type("ratio")
                            .diff(0.0) // 변화 없음
                            .build());
                }
            }

            // 변화량(절대값) 기준 내림차순 정렬 후 상위 3개 추출
            List<AiApiDto.Metric> top3Metrics = diffMetrics.stream()
                    .sorted((m1, m2) -> Double.compare(Math.abs(m2.getDiff()), Math.abs(m1.getDiff())))
                    .limit(3)
                    .collect(Collectors.toList());

            // 결과 객체에 덮어쓰기
            if (currentResult.getGrowth_report() == null) {
                currentResult.setGrowth_report(new AiApiDto.GrowthReport());
            }
            currentResult.getGrowth_report().setCurrent_metrics(top3Metrics);

        } catch (Exception e) {
            log.error("성장 리포트 메트릭 계산 중 오류", e);
            // 오류 나도 분석 전체를 실패처리하진 않고, 계산 안 된 상태로 넘어감
        }
    }

    private void saveAnalysisResult(Video video, AiApiDto.AiResult result) {
        AnalysisReport report = new AnalysisReport();
        report.setVideo(video);
        report.setUserId(video.getUserId());
        report.setChildId(video.getChildId());

        // 1. 점수 저장 및 QI 계산
        if (result.getScores() != null) {
            BigDecimal pi = BigDecimal.valueOf(result.getScores().getPi_score());
            BigDecimal ndi = BigDecimal.valueOf(result.getScores().getNdi_score());

            report.setPiScore(pi);
            report.setNdiScore(ndi);

            BigDecimal qi = calculateQiScore(pi, ndi);
            report.setQiScore(qi);
        }

        // 2. 관계 상태 저장 (Stage Name)
        if (result.getSummary_diagnosis() != null) {
            report.setRelationshipStatus(result.getSummary_diagnosis().getStage_name());
        }

        // 3. 전체 결과 JSON 저장 (프론트에서 상세 리포트 보여줄 때 사용)
        try {
            report.setContent(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.error("JSON 변환 오류", e);
        }

        reportRepository.save(report);

        // 4. 챌린지 정보 저장
        saveNewChallenge(video, result);

        // 5. 영상 상태 완료 처리
        video.setStatus(VideoStatus.COMPLETED);
        videoRepository.save(video);
    }

    // QI 점수 계산 메서드
    private BigDecimal calculateQiScore(BigDecimal pi, BigDecimal ndi) {
        BigDecimal total = pi.add(ndi);

        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return pi.divide(total, 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    // 챌린지 저장 메서드
    private void saveNewChallenge(Video video, AiApiDto.AiResult result) {
        if (result.getCoaching_and_plan() == null ||
                result.getCoaching_and_plan().getCoaching_plan() == null ||
                result.getCoaching_and_plan().getCoaching_plan().getChallenge() == null) {
            return;
        }

        AiApiDto.GeneratedChallenge aiChallenge = result.getCoaching_and_plan().getCoaching_plan().getChallenge();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate startDate = LocalDate.now(); // 기본값 오늘
        LocalDate endDate = startDate.plusDays(6); // 기본값 7일 후

        if (aiChallenge.getSuggested_period() != null) {
            try {
                startDate = LocalDate.parse(aiChallenge.getSuggested_period().getStart(), formatter);
                endDate = LocalDate.parse(aiChallenge.getSuggested_period().getEnd(), formatter);
            } catch (Exception e) {
                log.warn("날짜 파싱 실패, 기본값 사용: {}", e.getMessage());
            }
        }

        // User, Child 참조 가져오기 (ID 기반 프록시 조회)
        User userRef = userRepository.getReferenceById(video.getUserId());
        Child childRef = childRepository.getReferenceById(video.getChildId());

        Challenge challenge = Challenge.builder()
                .user(userRef)
                .child(childRef)
                .title(aiChallenge.getTitle())
                .goal(aiChallenge.getGoal())
                .startDate(startDate)
                .endDate(endDate)
                .status(ChallengeStatus.PROCEEDING)
                .build();

        if (aiChallenge.getActions() != null) {
            List<ChallengeAction> actions = aiChallenge.getActions().stream()
                    .map(content -> ChallengeAction.builder()
                            .challenge(challenge)
                            .content(content)
                            .isCompleted(false) // 초기 상태 미완료
                            .build())
                    .collect(Collectors.toList());
            challenge.setActions(actions);
        }

        challengeRepository.save(challenge);
        log.info("새로운 챌린지 생성 완료: {}", challenge.getTitle());
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
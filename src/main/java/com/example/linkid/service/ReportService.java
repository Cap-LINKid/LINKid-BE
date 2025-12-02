package com.example.linkid.service;

import com.example.linkid.domain.AnalysisReport;
import com.example.linkid.domain.Challenge;
import com.example.linkid.domain.Child;
import com.example.linkid.domain.User;
import com.example.linkid.dto.AiApiDto;
import com.example.linkid.dto.ReportDto;
import com.example.linkid.repository.AnalysisReportRepository;
import com.example.linkid.repository.ChallengeRepository;
import com.example.linkid.repository.ChildRepository;
import com.example.linkid.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final UserRepository userRepository;
    private final ChildRepository childRepository;
    private final AnalysisReportRepository reportRepository;
    private final ChallengeRepository challengeRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<ReportDto.ReportListResponse> getReportList(String username) {
        User user = getUser(username);
        Child child = getChild(user);

        List<AnalysisReport> reports = reportRepository.findAllByChildIdOrderByCreatedAtDesc(child.getChildId());

        return reports.stream()
                .map(report -> ReportDto.ReportListResponse.builder()
                        .reportId(report.getReportId())
                        .createdAt(report.getCreatedAt())
                        .contextTag(report.getVideo().getContextTag())
                        .durationSeconds(report.getVideo().getDuration() != null ? report.getVideo().getDuration() : 0) // 저장된 duration 사용
                        .relationshipStatus(report.getRelationshipStatus())
                        .piScore(report.getPiScore())
                        .ndiScore(report.getNdiScore())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReportDto.ReportDetailResponse getReportDetail(Long reportId, String username) {
        User user = getUser(username);

        AnalysisReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("리포트를 찾을 수 없습니다."));

        if (!report.getUserId().equals(user.getUserId())) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }

        // Content JSON 파싱
        AiApiDto.AiResult aiContent = null;
        try {
            if (report.getContent() != null && !report.getContent().isEmpty()) {
                aiContent = objectMapper.readValue(report.getContent(), AiApiDto.AiResult.class);
            }
        } catch (Exception e) {
            log.error("리포트 상세 조회 중 JSON 파싱 오류: reportId={}", reportId, e);
        }

        Optional<Challenge> challengeOpt = challengeRepository.findBySourceReport_ReportId(reportId);

        String status = "NOT_CREATED";
        Long challengeId = null;

        if (challengeOpt.isPresent()) {
            Challenge challenge = challengeOpt.get();
            status = challenge.getStatus().name(); // "PROCEEDING", "COMPLETED", "FAILED"
            challengeId = challenge.getChallengeId();
        }

        return ReportDto.ReportDetailResponse.builder()
                .reportId(report.getReportId())
                .createdAt(report.getCreatedAt())
                .username(user.getName())
                .content(aiContent)
                .challengeStatus(status)
                .challengeId(challengeId)
                .build();
    }

    private User getUser(String username) {
        return userRepository.findByLoginId(username)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));
    }

    private Child getChild(User user) {
        return childRepository.findFirstByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("자녀를 찾을 수 없습니다."));
    }
}
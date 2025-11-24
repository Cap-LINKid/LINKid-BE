package com.example.linkid.service;

import com.example.linkid.domain.*;
import com.example.linkid.dto.AiApiDto;
import com.example.linkid.dto.ChallengeDto;
import com.example.linkid.repository.AnalysisReportRepository;
import com.example.linkid.repository.ChallengeRepository;
import com.example.linkid.repository.ChildRepository;
import com.example.linkid.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChallengeService {

    private final ChallengeRepository challengeRepository;
    private final AnalysisReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ChildRepository childRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<ChallengeDto.ChallengeListResponse> getChallengeList(String username, String statusParam) {
        User user = getUser(username);
        Child child = getChild(user);

        ChallengeStatus status = "COMPLETED".equalsIgnoreCase(statusParam)
                ? ChallengeStatus.COMPLETED : ChallengeStatus.PROCEEDING;

        List<Challenge> challenges = challengeRepository.findAllByChildIdAndStatus(child.getChildId(), status);

        return challenges.stream().map(c -> {
            // 진행률 계산
            int total = c.getActions().size();
            int done = (int) c.getActions().stream().filter(ChallengeAction::isCompleted).count();
            int percent = total == 0 ? 0 : (done * 100 / total);

            return ChallengeDto.ChallengeListResponse.builder()
                    .challengeId(c.getChallengeId())
                    .title(c.getTitle())
                    .period(formatPeriod(c.getStartDate(), c.getEndDate()))
                    .progressPercent(percent)
                    .status(c.getStatus().name())
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ChallengeDto.ChallengeDetailResponse getChallengeDetail(Long challengeId, String username) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("챌린지를 찾을 수 없습니다."));

        int total = challenge.getActions().size();
        int done = (int) challenge.getActions().stream().filter(ChallengeAction::isCompleted).count();
        int percent = total == 0 ? 0 : (done * 100 / total);

        // 실천 목록 변환
        List<ChallengeDto.ActionItem> actionItems = challenge.getActions().stream()
                .map(a -> ChallengeDto.ActionItem.builder()
                        .actionId(a.getActionId())
                        .content(a.getContent())
                        .isCompleted(a.isCompleted())
                        .completedDate(a.getCompletedAt() != null ?
                                a.getCompletedAt().format(DateTimeFormatter.ofPattern("M월 d일")) : null)
                        .reflection(a.getReflection())
                        .build())
                .collect(Collectors.toList());

        return ChallengeDto.ChallengeDetailResponse.builder()
                .challengeId(challenge.getChallengeId())
                .title(challenge.getTitle())
                .goal(challenge.getGoal())
                .period(formatPeriod(challenge.getStartDate(), challenge.getEndDate()))
                .progressPercent(percent)
                .currentCount(done)
                .totalCount(total)
                .relatedReportId(challenge.getSourceReport().getReportId())
                .actions(actionItems)
                .build();
    }

    @Transactional
    public Long createChallengeFromReport(Long reportId, String username) {
        User user = getUser(username);
        Child child = getChild(user);

        AnalysisReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("리포트를 찾을 수 없습니다."));

        try {
            AiApiDto.AiResult result = objectMapper.readValue(report.getContent(), AiApiDto.AiResult.class);

            if (result.getCoaching_and_plan() == null || result.getCoaching_and_plan().getCoaching_plan() == null) {
                throw new IllegalArgumentException("챌린지 데이터가 없습니다.");
            }

            var plan = result.getCoaching_and_plan().getCoaching_plan();
            var aiChallenge = plan.getChallenge();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate startDate = LocalDate.now();
            LocalDate endDate = startDate.plusDays(6);
            if (aiChallenge.getSuggested_period() != null) {
                startDate = LocalDate.parse(aiChallenge.getSuggested_period().getStart(), formatter);
                endDate = LocalDate.parse(aiChallenge.getSuggested_period().getEnd(), formatter);
            }

            Challenge challenge = Challenge.builder()
                    .user(user)
                    .child(child)
                    .sourceReport(report)
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
                                .isCompleted(false)
                                .build())
                        .collect(Collectors.toList());
                challenge.setActions(actions);
            }

            challengeRepository.save(challenge);
            return challenge.getChallengeId();

        } catch (Exception e) {
            throw new RuntimeException("챌린지 생성 실패", e);
        }
    }

    private String formatPeriod(LocalDate start, LocalDate end) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("M월 d일");
        return start.format(fmt) + " ~ " + end.format(fmt);
    }

    private User getUser(String username) {
        return userRepository.findByName(username).orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private Child getChild(User user) {
        return childRepository.findFirstByUser(user).orElseThrow(() -> new IllegalArgumentException("Child not found"));
    }
}
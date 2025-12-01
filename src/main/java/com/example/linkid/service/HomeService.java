package com.example.linkid.service;

import com.example.linkid.domain.AnalysisReport;
import com.example.linkid.domain.Challenge;
import com.example.linkid.domain.ChallengeStatus;
import com.example.linkid.domain.Child;
import com.example.linkid.domain.User;
import com.example.linkid.dto.HomeDto;
import com.example.linkid.repository.AnalysisReportRepository;
import com.example.linkid.repository.ChallengeRepository;
import com.example.linkid.repository.ChildRepository;
import com.example.linkid.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final UserRepository userRepository;
    private final ChildRepository childRepository;
    private final AnalysisReportRepository reportRepository;
    private final ChallengeRepository challengeRepository;

    @Transactional(readOnly = true)
    public HomeDto.HomeResponse getHomeData(String username) {
        User user = userRepository.findByLoginId(username)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));
        Child child = childRepository.findFirstByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("자녀를 찾을 수 없습니다."));

        // 성장 리포트 데이터 조회 (최근 5개)
        List<AnalysisReport> recentReports = reportRepository.findTop5ByChildIdOrderByCreatedAtDesc(child.getChildId());

        // 날짜 오름차순으로 재정렬
        recentReports.sort(Comparator.comparing(AnalysisReport::getCreatedAt));

        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("MM.dd");

        List<HomeDto.QiScorePoint> qiHistory = recentReports.stream()
                .map(r -> new HomeDto.QiScorePoint(
                        r.getCreatedAt().format(dateFmt),
                        r.getQiScore() != null ? r.getQiScore().intValue() : 0
                ))
                .collect(Collectors.toList());

        List<HomeDto.PiNdiPoint> piNdiHistory = recentReports.stream()
                .map(r -> new HomeDto.PiNdiPoint(
                        r.getCreatedAt().format(dateFmt),
                        r.getPiScore() != null ? r.getPiScore().intValue() : 0,
                        r.getNdiScore() != null ? r.getNdiScore().intValue() : 0
                ))
                .collect(Collectors.toList());

        HomeDto.GrowthReport growthReport = HomeDto.GrowthReport.builder()
                .qiScoreHistory(qiHistory)
                .piNdiHistory(piNdiHistory)
                .build();

        // 이번 주 핵심 챌린지 조회 (진행 중인 최신 1개)
        Optional<Challenge> activeChallengeOpt = challengeRepository
                .findFirstByChildChildIdAndStatusOrderByCreatedAtDesc(child.getChildId(), ChallengeStatus.PROCEEDING);

        HomeDto.ActiveChallenge activeChallengeDto = null;
        if (activeChallengeOpt.isPresent()) {
            Challenge c = activeChallengeOpt.get();
            DateTimeFormatter periodFmt = DateTimeFormatter.ofPattern("yyyy.MM.dd.");
            String periodStr = c.getStartDate().format(periodFmt) + " - " + c.getEndDate().format(periodFmt);

            activeChallengeDto = HomeDto.ActiveChallenge.builder()
                    .challengeId(c.getChallengeId())
                    .title(c.getTitle())
                    .goal(c.getGoal())
                    .period(periodStr)
                    .build();
        }

        return HomeDto.HomeResponse.builder()
                .growthReport(growthReport)
                .activeChallenge(activeChallengeDto)
                .build();
    }
}
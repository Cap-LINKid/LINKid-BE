package com.example.linkid.scheduler;

import com.example.linkid.domain.Challenge;
import com.example.linkid.domain.ChallengeStatus;
import com.example.linkid.repository.ChallengeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChallengeScheduler {

    private final ChallengeRepository challengeRepository;

    // 매일 자정(00:00:00)에 실행
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void checkFailedChallenges() {
        log.info("기간 만료 챌린지 점검 시작...");

        // 종료일(endDate)이 어제보다 이전(즉, 오늘 기준 이미 지난)이면서
        // 상태가 아직 '진행 중(PROCEEDING)'인 챌린지 조회
        LocalDate yesterday = LocalDate.now().minusDays(1);
        List<Challenge> expiredChallenges = challengeRepository.findAllByStatusAndEndDateBefore(
                ChallengeStatus.PROCEEDING,
                LocalDate.now() // 오늘 날짜보다 이전인 것들 (어제까지 끝냈어야 함)
        );

        for (Challenge challenge : expiredChallenges) {
            challenge.setStatus(ChallengeStatus.FAILED);
        }

        log.info("총 {}개의 기간 만료 챌린지를 실패(FAILED) 처리했습니다.", expiredChallenges.size());
    }
}
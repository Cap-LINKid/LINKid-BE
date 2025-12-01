package com.example.linkid.service;

import com.example.linkid.domain.Child;
import com.example.linkid.domain.User;
import com.example.linkid.dto.MyPageDto;
import com.example.linkid.repository.AnalysisReportRepository;
import com.example.linkid.repository.ChallengeRepository;
import com.example.linkid.repository.ChildRepository;
import com.example.linkid.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MyPageService {

    private final UserRepository userRepository;
    private final ChildRepository childRepository;
    private final AnalysisReportRepository reportRepository;
    private final ChallengeRepository challengeRepository;

    @Transactional(readOnly = true)
    public MyPageDto.MyPageResponse getMyPageInfo(String username) {
        User user = userRepository.findByLoginId(username)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."));

        Child child = childRepository.findFirstByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("자녀를 찾을 수 없습니다."));

        MyPageDto.ChildInfo childInfo = MyPageDto.ChildInfo.builder()
                .childId(child.getChildId())
                .name(child.getName())
                .birthdate(child.getBirthdate())
                .gender(child.getGender().name())
                .build();

        int reportCount = reportRepository.countByChildId(child.getChildId());
        int challengeCount = challengeRepository.countByChildChildId(child.getChildId());

        MyPageDto.ActivitySummary summary = MyPageDto.ActivitySummary.builder()
                .totalReports(reportCount)
                .totalChallenges(challengeCount)
                .build();

        return MyPageDto.MyPageResponse.builder()
                .userName(user.getName())
                .childInfo(childInfo)
                .activitySummary(summary)
                .build();
    }
}
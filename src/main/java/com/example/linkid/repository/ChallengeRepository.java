package com.example.linkid.repository;

import com.example.linkid.domain.Challenge;
import com.example.linkid.domain.ChallengeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChallengeRepository extends JpaRepository<Challenge, Long> {

    // 특정 자녀의 진행 중인 챌린지 목록 조회 (행동까지 같이 가져오기 위해 fetch join)
    @Query("SELECT c FROM Challenge c JOIN FETCH c.actions WHERE c.child.childId = :childId AND c.status = 'PROCEEDING'")
    List<Challenge> findActiveChallengesByChildId(@Param("childId") Long childId);
}
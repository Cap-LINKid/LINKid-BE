package com.example.linkid.repository;

import com.example.linkid.domain.ChallengeAction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChallengeActionRepository extends JpaRepository<ChallengeAction, Long> {
}
package com.example.linkid.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ChallengeAction {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long actionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id")
    private Challenge challenge;

    @Column(nullable = false)
    private String content; // 행동 내용 (예: "아이의 말 따라하기")

    private boolean isCompleted; // 행동별 완료 여부

    private LocalDateTime completedAt;

    @Column(columnDefinition = "TEXT")
    private String reflection; // 회고 (선택 사항)
}
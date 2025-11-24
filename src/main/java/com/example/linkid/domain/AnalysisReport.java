package com.example.linkid.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor
public class AnalysisReport extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reportId;

    @OneToOne
    @JoinColumn(name = "video_id")
    private Video video;

    private Long userId;
    private Long childId;

    private String relationshipStatus; // 공감적 협력 등
    private BigDecimal piScore;
    private BigDecimal ndiScore;
    private BigDecimal qiScore;

    @Column(columnDefinition = "TEXT") // JSON 형태로 저장
    private String content;
}
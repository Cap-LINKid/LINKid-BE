package com.example.linkid.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor
public class Video extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long videoId;

    private Long userId;
    private Long childId;

    private String fileName;
    private String bucketKey;
    private String contentType;
    private String contextTag;

    @Column(nullable = false)
    private String originalVideoUrl;

    @Enumerated(EnumType.STRING)
    private VideoStatus status = VideoStatus.UPLOADING;

    @Column(columnDefinition = "TEXT")
    private String sttResult;

    private String aiExecutionId;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private LocalDateTime statusUpdatedAt = LocalDateTime.now();
}
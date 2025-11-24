package com.example.linkid.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "child")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Child extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long childId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 100)
    private String name;

    private LocalDate birthdate;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender = Gender.OTHER;

    @Column(columnDefinition = "json")
    private String parentingStyle;

    public enum Gender {
        MALE, FEMALE, OTHER
    }
}
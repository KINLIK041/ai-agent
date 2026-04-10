package com.kinlik.aicodehelper.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "mood_record")
@Data
public class MoodRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private LocalDate recordDate;

    private String emotionType;

    private Integer emotionIntensity;

    @Column(length = 2000)
    private String emotionDescription;

    @Column(length = 3000)
    private String triggerEvent;

    @Column(length = 3000)
    private String aiSuggestion;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
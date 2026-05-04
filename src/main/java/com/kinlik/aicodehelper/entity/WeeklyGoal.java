package com.kinlik.aicodehelper.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "weekly_goal")
@Data
public class WeeklyGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private LocalDate weekStart;

    @Enumerated(EnumType.STRING)
    private GoalType type;

    private int targetCount;

    private int currentCount;

    private boolean completed;

    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        if (weekStart == null) {
            weekStart = LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1);
        }
    }

    public enum GoalType {
        GRATITUDE,
        MINDFULNESS,
        EXERCISE,
        SOCIAL,
        CONSISTENCY
    }
}

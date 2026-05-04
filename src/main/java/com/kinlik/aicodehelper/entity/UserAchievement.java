package com.kinlik.aicodehelper.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_achievement")
@Data
public class UserAchievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private String badgeId;

    private LocalDateTime unlockedAt;

    private boolean viewed;

    @PrePersist
    protected void onCreate() {
        unlockedAt = LocalDateTime.now();
        viewed = false;
    }
}

package com.cubecrush.game.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "scores")
@Data
public class Score {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Integer score;

    @Column(name = "achieved_at")
    private LocalDateTime achievedAt;

    @PrePersist
    protected void onCreate() {
        if (achievedAt == null) {
            achievedAt = LocalDateTime.now();
        }
    }
}

package com.cubecrush.game.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

@Entity
@Immutable
@Table(name = "user_stats")
@Data
public class UserStats {
    @Id
    private Long id;

    private String nickname;
    private LocalDateTime bestScoreAchievedAt;
    private Long gamesPlayed;
    private Integer bestScore;
    private Double averageScore;
}

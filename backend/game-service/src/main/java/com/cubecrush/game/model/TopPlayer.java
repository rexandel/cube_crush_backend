package com.cubecrush.game.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

@Entity
@Immutable
@Table(name = "top_players")
@Data
public class TopPlayer {
    @Id
    private Long id;

    private String nickname;
    private Integer score;
    private LocalDateTime achievedAt;
}

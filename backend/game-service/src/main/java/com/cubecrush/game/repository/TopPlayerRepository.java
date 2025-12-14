package com.cubecrush.game.repository;

import com.cubecrush.game.model.TopPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TopPlayerRepository extends JpaRepository<TopPlayer, Long> {
    List<TopPlayer> findAllByOrderByScoreDesc();
}

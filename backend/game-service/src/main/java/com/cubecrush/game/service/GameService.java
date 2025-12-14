package com.cubecrush.game.service;

import com.cubecrush.game.model.Score;
import com.cubecrush.game.model.TopPlayer;
import com.cubecrush.game.model.UserStats;
import com.cubecrush.game.repository.ScoreRepository;
import com.cubecrush.game.repository.TopPlayerRepository;
import com.cubecrush.game.repository.UserStatsRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GameService {

    private final ScoreRepository scoreRepository;
    private final TopPlayerRepository topPlayerRepository;
    private final UserStatsRepository userStatsRepository;
    private final RestTemplate restTemplate;

    @Transactional
    public Score submitScore(Long userId, Integer scoreValue) {
        Score score = new Score();
        score.setUserId(userId);
        score.setScore(scoreValue);
        return scoreRepository.save(score);
    }

    public List<TopPlayer> getTopPlayers() {
        return topPlayerRepository.findAllByOrderByScoreDesc();
    }

    public UserStats getUserStats(Long userId) {
        UserStats stats = userStatsRepository.findById(userId)
                .orElseGet(() -> {
                    UserStats emptyStats = new UserStats();
                    emptyStats.setId(userId);
                    emptyStats.setGamesPlayed(0L);
                    emptyStats.setBestScore(null);
                    emptyStats.setAverageScore(null);
                    emptyStats.setBestScoreAchievedAt(null);
                    return emptyStats;
                });

        if (stats.getNickname() == null) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.set("X-User-Id", String.valueOf(userId));
                HttpEntity<Void> entity = new HttpEntity<>(headers);

                ResponseEntity<UserProfileDto> response = restTemplate.exchange(
                        "http://user-service/api/v1/users/me",
                        HttpMethod.GET,
                        entity,
                        UserProfileDto.class
                );

                if (response.getBody() != null) {
                    stats.setNickname(response.getBody().getNickname());
                }
            } catch (Exception e) {
                // Log error or ignore (nickname will remain null)
                System.err.println("Failed to fetch nickname for user " + userId + ": " + e.getMessage());
            }
        }
        return stats;
    }

    public List<Score> getUserHistory(Long userId) {
        return scoreRepository.findByUserIdOrderByAchievedAtDesc(userId);
    }

    @Data
    private static class UserProfileDto {
        private Long id;
        private String nickname;
    }
}

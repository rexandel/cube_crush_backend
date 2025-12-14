package com.cubecrush.game.web;

import com.cubecrush.game.model.Score;
import com.cubecrush.game.model.TopPlayer;
import com.cubecrush.game.model.UserStats;
import com.cubecrush.game.service.GameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/game")
@RequiredArgsConstructor
@Tag(name = "Game", description = "Game logic endpoints")
public class GameController {

    private final GameService gameService;

    @PostMapping("/score")
    @Operation(summary = "Submit a new score", description = "Saves a new score for the authenticated user. Updates user stats and global leaderboard if applicable.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "200", description = "Score saved successfully")
    public ResponseEntity<Score> submitScore(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId, @Valid @RequestBody ScoreRequest request) {
        return ResponseEntity.ok(gameService.submitScore(userId, request.getScore()));
    }

    @GetMapping("/top")
    @Operation(summary = "Get global leaderboard", description = "Returns the list of top players sorted by score. Includes all players who have ever played.")
    @ApiResponse(responseCode = "200", description = "Leaderboard retrieved successfully")
    public ResponseEntity<List<TopPlayer>> getTopPlayers() {
        return ResponseEntity.ok(gameService.getTopPlayers());
    }

    @GetMapping("/stats")
    @Operation(summary = "Get user statistics", description = "Returns statistics for the authenticated user (games played, best score, average score).")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    public ResponseEntity<UserStats> getUserStats(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(gameService.getUserStats(userId));
    }

    @GetMapping("/history")
    @Operation(summary = "Get score history", description = "Returns the history of scores for the authenticated user.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "200", description = "History retrieved successfully")
    public ResponseEntity<List<Score>> getUserHistory(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(gameService.getUserHistory(userId));
    }
}

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(100) UNIQUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS scores (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    score INTEGER NOT NULL CHECK (score >= 0),
    achieved_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    jti VARCHAR(255) UNIQUE NOT NULL,
    refresh_token_hash VARCHAR(255) UNIQUE NOT NULL,
    access_token_hash VARCHAR(255),
    access_token_expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    refresh_token_expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    is_revoked BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS revoked_tokens (
    jti VARCHAR(255) PRIMARY KEY,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_revoked_tokens_expires ON revoked_tokens(expires_at);
CREATE INDEX IF NOT EXISTS idx_user_sessions_jti ON user_sessions(jti);
CREATE INDEX IF NOT EXISTS idx_user_sessions_user_id ON user_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_user_sessions_access_expires ON user_sessions(access_token_expires_at);
CREATE INDEX IF NOT EXISTS idx_user_sessions_refresh_expires ON user_sessions(refresh_token_expires_at);
CREATE INDEX IF NOT EXISTS idx_user_sessions_refresh_token ON user_sessions(refresh_token_hash);
CREATE INDEX IF NOT EXISTS idx_user_sessions_revoked ON user_sessions(is_revoked) WHERE is_revoked = false;

CREATE INDEX IF NOT EXISTS idx_users_nickname ON users(nickname);
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at);

CREATE INDEX IF NOT EXISTS idx_scores_user_id ON scores(user_id);
CREATE INDEX IF NOT EXISTS idx_scores_score ON scores(score DESC);
CREATE INDEX IF NOT EXISTS idx_scores_achieved_at ON scores(achieved_at);
CREATE INDEX IF NOT EXISTS idx_scores_user_score ON scores(user_id, score DESC);
CREATE INDEX IF NOT EXISTS idx_scores_user_achieved ON scores(user_id, achieved_at DESC);

CREATE MATERIALIZED VIEW IF NOT EXISTS top_players AS
SELECT 
    u.id,
    u.nickname,
    MAX(s.score) as score,
    MAX(s.achieved_at) as achieved_at
FROM scores s
JOIN users u ON u.id = s.user_id
GROUP BY u.id, u.nickname
ORDER BY score DESC;

CREATE MATERIALIZED VIEW IF NOT EXISTS user_stats AS
SELECT 
    u.id,
    u.nickname,
    (
        SELECT achieved_at 
        FROM scores s2 
        WHERE s2.user_id = u.id 
        ORDER BY s2.score DESC, s2.achieved_at DESC 
        LIMIT 1
    ) as best_score_achieved_at,
    COUNT(s.id) as games_played,
    MAX(s.score) as best_score,
    ROUND(AVG(s.score)) as average_score
FROM users u
LEFT JOIN scores s ON u.id = s.user_id
GROUP BY u.id, u.nickname;

CREATE UNIQUE INDEX IF NOT EXISTS idx_top_players_user_id ON top_players(id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_stats_user_id ON user_stats(id);
CREATE INDEX IF NOT EXISTS idx_top_players_score ON top_players(score DESC);
CREATE INDEX IF NOT EXISTS idx_user_stats_best_score ON user_stats(best_score DESC NULLS LAST);

CREATE OR REPLACE FUNCTION refresh_game_views()
RETURNS TRIGGER AS $$
DECLARE
    current_best INTEGER;
BEGIN
    REFRESH MATERIALIZED VIEW user_stats;
    
    SELECT MAX(score) INTO current_best FROM scores WHERE user_id = NEW.user_id;
    
    IF NEW.score >= COALESCE(current_best, 0) THEN
        REFRESH MATERIALIZED VIEW top_players;
    END IF;
    
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS refresh_views_after_score ON scores;
CREATE TRIGGER refresh_views_after_score
    AFTER INSERT OR UPDATE OR DELETE ON scores
    FOR EACH ROW
    EXECUTE FUNCTION refresh_game_views();

CREATE OR REPLACE FUNCTION get_user_score_history(user_id_param INTEGER)
RETURNS TABLE(score INTEGER, achieved_at TIMESTAMP WITH TIME ZONE) AS $$
BEGIN
    RETURN QUERY
    SELECT s.score, s.achieved_at
    FROM scores s
    WHERE s.user_id = user_id_param
    ORDER BY s.achieved_at DESC;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION is_new_best_score(user_id_param INTEGER, new_score INTEGER)
RETURNS BOOLEAN AS $$
DECLARE
    current_best INTEGER;
BEGIN
    SELECT MAX(score) INTO current_best
    FROM scores
    WHERE user_id = user_id_param;
    
    RETURN current_best IS NULL OR new_score > current_best;
END;
$$ LANGUAGE plpgsql;

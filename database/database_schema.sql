-- Основные таблицы
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(100) UNIQUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS high_scores (
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

-- Индексы для оптимизации
CREATE INDEX IF NOT EXISTS idx_revoked_tokens_expires ON revoked_tokens(expires_at);
CREATE INDEX IF NOT EXISTS idx_user_sessions_jti ON user_sessions(jti);
CREATE INDEX IF NOT EXISTS idx_user_sessions_user_id ON user_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_user_sessions_access_expires ON user_sessions(access_token_expires_at);
CREATE INDEX IF NOT EXISTS idx_user_sessions_refresh_expires ON user_sessions(refresh_token_expires_at);
CREATE INDEX IF NOT EXISTS idx_user_sessions_refresh_token ON user_sessions(refresh_token_hash);
CREATE INDEX IF NOT EXISTS idx_user_sessions_revoked ON user_sessions(is_revoked) WHERE is_revoked = false;

CREATE INDEX IF NOT EXISTS idx_users_nickname ON users(nickname);
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at);

CREATE INDEX IF NOT EXISTS idx_high_scores_user_id ON high_scores(user_id);
CREATE INDEX IF NOT EXISTS idx_high_scores_score ON high_scores(score DESC);
CREATE INDEX IF NOT EXISTS idx_high_scores_achieved_at ON high_scores(achieved_at);
CREATE INDEX IF NOT EXISTS idx_high_scores_user_score ON high_scores(user_id, score DESC);
CREATE INDEX IF NOT EXISTS idx_high_scores_user_achieved ON high_scores(user_id, achieved_at DESC);

-- Materialized Views для статистики
CREATE MATERIALIZED VIEW IF NOT EXISTS top_players AS
SELECT 
    u.id,
    u.nickname,
    MAX(hs.score) as score,
    MAX(hs.achieved_at) as achieved_at
FROM high_scores hs
JOIN users u ON u.id = hs.user_id
GROUP BY u.id, u.nickname
ORDER BY score DESC
LIMIT 10;

CREATE MATERIALIZED VIEW IF NOT EXISTS user_stats AS
SELECT 
    u.id,
    u.nickname,
    u.created_at,
    COUNT(hs.id) as games_played,
    MAX(hs.score) as best_score,
    ROUND(AVG(hs.score)) as average_score
FROM users u
LEFT JOIN high_scores hs ON u.id = hs.user_id
GROUP BY u.id, u.nickname, u.created_at;

-- Индексы для материализованных представлений
CREATE UNIQUE INDEX IF NOT EXISTS idx_top_players_user_id ON top_players(id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_stats_user_id ON user_stats(id);
CREATE INDEX IF NOT EXISTS idx_top_players_score ON top_players(score DESC);
CREATE INDEX IF NOT EXISTS idx_user_stats_best_score ON user_stats(best_score DESC NULLS LAST);

-- Функции для обновления материализованных представлений
CREATE OR REPLACE FUNCTION refresh_game_views()
RETURNS TRIGGER AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY top_players;
    REFRESH MATERIALIZED VIEW CONCURRENTLY user_stats;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Триггеры для автоматического обновления представлений
DROP TRIGGER IF EXISTS refresh_views_after_score ON high_scores;
CREATE TRIGGER refresh_views_after_score
    AFTER INSERT OR UPDATE OR DELETE ON high_scores
    FOR EACH STATEMENT
    EXECUTE FUNCTION refresh_game_views();

-- Функция для получения истории счетов пользователя
CREATE OR REPLACE FUNCTION get_user_score_history(user_id_param INTEGER)
RETURNS TABLE(score INTEGER, achieved_at TIMESTAMP WITH TIME ZONE) AS $$
BEGIN
    RETURN QUERY
    SELECT hs.score, hs.achieved_at
    FROM high_scores hs
    WHERE hs.user_id = user_id_param
    ORDER BY hs.achieved_at DESC;
END;
$$ LANGUAGE plpgsql;

-- Функция для проверки, является ли счет новым рекордом
CREATE OR REPLACE FUNCTION is_new_best_score(user_id_param INTEGER, new_score INTEGER)
RETURNS BOOLEAN AS $$
DECLARE
    current_best INTEGER;
BEGIN
    SELECT MAX(score) INTO current_best
    FROM high_scores
    WHERE user_id = user_id_param;
    
    RETURN current_best IS NULL OR new_score > current_best;
END;
$$ LANGUAGE plpgsql;
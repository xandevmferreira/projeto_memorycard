CREATE TABLE game_screenshots (
    id          BIGSERIAL PRIMARY KEY,
    game_id     BIGINT       NOT NULL REFERENCES games (id) ON DELETE CASCADE,
    file_path   VARCHAR(500) NOT NULL,
    uploaded_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_game_screenshots_game_id ON game_screenshots (game_id);

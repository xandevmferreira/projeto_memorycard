ALTER TABLE users
    ADD COLUMN community_visible BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN retro_achievements_username VARCHAR(100),
    ADD COLUMN retro_achievements_api_key VARCHAR(255);

ALTER TABLE games
    ADD COLUMN completion_type VARCHAR(30),
    ADD COLUMN tags VARCHAR(500),
    ADD COLUMN is_retro BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN emulator VARCHAR(100),
    ADD COLUMN retro_achievements_game_id INTEGER,
    ADD COLUMN retro_console_id INTEGER,
    ADD COLUMN retro_progress_percent DECIMAL(5, 2);

CREATE TABLE game_lists (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name        VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE game_list_items (
    id       BIGSERIAL PRIMARY KEY,
    list_id  BIGINT NOT NULL REFERENCES game_lists (id) ON DELETE CASCADE,
    game_id  BIGINT NOT NULL REFERENCES games (id) ON DELETE CASCADE,
    position INTEGER NOT NULL DEFAULT 0,
    UNIQUE (list_id, game_id)
);

CREATE TABLE game_journal_entries (
    id         BIGSERIAL PRIMARY KEY,
    game_id    BIGINT       NOT NULL REFERENCES games (id) ON DELETE CASCADE,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    content    TEXT         NOT NULL,
    spoiler    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_game_lists_user ON game_lists (user_id);
CREATE INDEX idx_game_journal_game ON game_journal_entries (game_id);
CREATE INDEX idx_games_user_retro ON games (user_id, is_retro);
CREATE INDEX idx_users_community ON users (community_visible) WHERE community_visible = TRUE;

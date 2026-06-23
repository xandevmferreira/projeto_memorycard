ALTER TABLE users
    ADD COLUMN share_notes_with_friends BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN share_journal_with_friends BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN share_screenshots_with_friends BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE play_sessions (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    game_id          BIGINT       NOT NULL REFERENCES games (id) ON DELETE CASCADE,
    started_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    ended_at         TIMESTAMPTZ,
    duration_minutes INTEGER,
    source           VARCHAR(30)  NOT NULL DEFAULT 'WEB',
    process_name     VARCHAR(255),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_play_sessions_user_game ON play_sessions (user_id, game_id);
CREATE INDEX idx_play_sessions_active ON play_sessions (user_id) WHERE ended_at IS NULL;

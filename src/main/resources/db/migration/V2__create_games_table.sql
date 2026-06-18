CREATE TABLE games (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title            VARCHAR(255) NOT NULL,
    platform         VARCHAR(100),
    status           VARCHAR(20)  NOT NULL DEFAULT 'PLAYING',
    hours_played     NUMERIC(8, 2) DEFAULT 0,
    personal_rating  NUMERIC(3, 1),
    external_rating  NUMERIC(3, 1),
    notes            TEXT,
    cover_url        VARCHAR(500),
    started_at       DATE,
    completed_at     DATE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_games_user_id ON games (user_id);
CREATE INDEX idx_games_status ON games (status);
CREATE INDEX idx_games_created_at ON games (created_at DESC);

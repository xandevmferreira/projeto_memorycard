CREATE TABLE game_cartridges (
    id BIGSERIAL PRIMARY KEY,
    game_id BIGINT NOT NULL REFERENCES games(id) ON DELETE CASCADE,
    label VARCHAR(120) NOT NULL,
    memories TEXT,
    session_date DATE,
    emulator_hint VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_game_cartridges_game ON game_cartridges(game_id);

CREATE TABLE game_archive_files (
    id BIGSERIAL PRIMARY KEY,
    cartridge_id BIGINT NOT NULL REFERENCES game_cartridges(id) ON DELETE CASCADE,
    game_id BIGINT NOT NULL REFERENCES games(id) ON DELETE CASCADE,
    file_type VARCHAR(20) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL DEFAULT 0,
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_game_archive_files_cartridge ON game_archive_files(cartridge_id);
CREATE INDEX idx_game_archive_files_game ON game_archive_files(game_id);

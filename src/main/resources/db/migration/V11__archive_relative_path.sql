ALTER TABLE game_archive_files
    ADD COLUMN IF NOT EXISTS relative_path VARCHAR(500);

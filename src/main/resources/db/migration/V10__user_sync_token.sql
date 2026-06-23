ALTER TABLE users ADD COLUMN sync_token_hash VARCHAR(64);
ALTER TABLE users ADD COLUMN sync_token_created_at TIMESTAMPTZ;

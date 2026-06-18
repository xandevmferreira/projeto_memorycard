ALTER TABLE games ALTER COLUMN external_rating TYPE NUMERIC(5, 1);
ALTER TABLE games ADD COLUMN IF NOT EXISTS rating_source VARCHAR(20);

CREATE TABLE friendships (
    id BIGSERIAL PRIMARY KEY,
    requester_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    addressee_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT friendships_unique_pair UNIQUE (requester_id, addressee_id),
    CONSTRAINT friendships_no_self CHECK (requester_id <> addressee_id)
);

CREATE INDEX idx_friendships_requester ON friendships(requester_id);
CREATE INDEX idx_friendships_addressee ON friendships(addressee_id);

CREATE TABLE badges (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    icon VARCHAR(10) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0
);

CREATE TABLE user_badges (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    badge_id BIGINT NOT NULL REFERENCES badges(id) ON DELETE CASCADE,
    earned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT user_badges_unique UNIQUE (user_id, badge_id)
);

INSERT INTO badges (code, name, description, icon, sort_order) VALUES
('FIRST_GAME', 'Primeiro jogo', 'Cadastrou o primeiro jogo na biblioteca', '🎮', 1),
('FIRST_COMPLETE', 'Primeira vitória', 'Zerou o primeiro jogo', '🏆', 2),
('COLLECTOR_5', 'Colecionador', '5 jogos na biblioteca', '📚', 3),
('COLLECTOR_10', 'Arquivo pessoal', '10 jogos na biblioteca', '🗂️', 4),
('COMPLETIONIST_3', 'Caçador de troféus', 'Zerou 3 jogos', '⭐', 5),
('COMPLETIONIST_10', 'Lenda do platinum', 'Zerou 10 jogos', '👑', 6),
('RETRO_FAN', 'Pixel lover', 'Cadastrou um jogo retro/emulador', '🕹️', 7),
('SOCIAL_START', 'Novato social', 'Adicionou o primeiro amigo', '🤝', 8),
('SOCIAL_3', 'Party up', 'Tem 3 amigos na rede', '👥', 9),
('COMMUNITY', 'Na vitrine', 'Ativou visibilidade na comunidade', '🌐', 10);

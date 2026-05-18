CREATE TABLE IF NOT EXISTS players (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    total_score INTEGER DEFAULT 0,
    tournaments_won INTEGER DEFAULT 0,
    money INTEGER DEFAULT 10000,
    created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tournaments (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    max_players INTEGER NOT NULL,
    current_players INTEGER DEFAULT 0,
    buy_in INTEGER NOT NULL,
    starting_chips INTEGER NOT NULL,
    host_id BIGINT,
    winner_id BIGINT
);

CREATE TABLE IF NOT EXISTS tournament_participants (
    id BIGSERIAL PRIMARY KEY,
    tournament_id BIGINT NOT NULL REFERENCES tournaments(id),
    player_id BIGINT NOT NULL REFERENCES players(id),
    status VARCHAR(50) NOT NULL,
    current_chips INTEGER NOT NULL,
    joined_at TIMESTAMP NOT NULL,
    eliminated_at TIMESTAMP,
    final_position INTEGER,
    UNIQUE(tournament_id, player_id)
);
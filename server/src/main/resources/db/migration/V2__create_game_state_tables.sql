-- Phase 1: Expanded Database Schema for Persistent Game State
-- This migration creates tables to store all game state in the database
-- instead of relying on in-memory storage that is lost on restart

-- Game States Table
-- Stores the current state of each tournament's game
CREATE TABLE game_states (
    id BIGSERIAL PRIMARY KEY,
    tournament_id BIGINT NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    phase VARCHAR(50) NOT NULL DEFAULT 'WAITING',
    current_player_id BIGINT REFERENCES players(id),
    dealer_position INTEGER DEFAULT 0,
    small_blind INTEGER DEFAULT 50,
    big_blind INTEGER DEFAULT 100,
    pot INTEGER DEFAULT 0,
    current_bet INTEGER DEFAULT 0,
    community_cards JSONB DEFAULT '[]'::jsonb,
    deck_state JSONB DEFAULT '[]'::jsonb,
    round_number INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(tournament_id)
);

CREATE INDEX idx_game_states_tournament_id ON game_states(tournament_id);
CREATE INDEX idx_game_states_phase ON game_states(phase);

-- Player Hands Table
-- Stores each player's hand and chip state for a tournament
CREATE TABLE player_hands (
    id BIGSERIAL PRIMARY KEY,
    tournament_id BIGINT NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    player_id BIGINT NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    cards JSONB DEFAULT '[]'::jsonb,
    current_chips INTEGER NOT NULL DEFAULT 0,
    current_bet INTEGER DEFAULT 0,
    total_bet_this_round INTEGER DEFAULT 0,
    folded BOOLEAN DEFAULT FALSE,
    all_in BOOLEAN DEFAULT FALSE,
    position INTEGER,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(tournament_id, player_id)
);

CREATE INDEX idx_player_hands_tournament_id ON player_hands(tournament_id);
CREATE INDEX idx_player_hands_player_id ON player_hands(player_id);
CREATE INDEX idx_player_hands_active ON player_hands(tournament_id, folded) WHERE folded = FALSE;

-- Alter tournament_participants to add ready column if not exists
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='tournament_participants' 
                   AND column_name='ready') THEN
        ALTER TABLE tournament_participants ADD COLUMN ready BOOLEAN DEFAULT FALSE;
    END IF;
END $$;

-- Game Actions Log Table (for debugging and replay)
CREATE TABLE game_actions (
    id BIGSERIAL PRIMARY KEY,
    tournament_id BIGINT NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    player_id BIGINT REFERENCES players(id),
    action_type VARCHAR(50) NOT NULL,
    amount INTEGER,
    phase VARCHAR(50),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_game_actions_tournament_id ON game_actions(tournament_id);
CREATE INDEX idx_game_actions_created_at ON game_actions(created_at);

-- Update tournaments table to add game-specific fields
ALTER TABLE tournaments ADD COLUMN IF NOT EXISTS small_blind INTEGER DEFAULT 50;
ALTER TABLE tournaments ADD COLUMN IF NOT EXISTS big_blind INTEGER DEFAULT 100;
ALTER TABLE tournaments ADD COLUMN IF NOT EXISTS current_round INTEGER DEFAULT 0;

COMMENT ON TABLE game_states IS 'Persistent storage of game state for each tournament';
COMMENT ON TABLE player_hands IS 'Player-specific game state including cards and chips';
COMMENT ON TABLE game_actions IS 'Audit log of all game actions for replay and debugging';

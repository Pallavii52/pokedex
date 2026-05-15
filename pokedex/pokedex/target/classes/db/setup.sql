CREATE DATABASE pokedex_db;

\c pokedex_db;

CREATE TABLE IF NOT EXISTS pokemon_cache (
    id BIGSERIAL PRIMARY KEY,
    pokemon_name VARCHAR(100) NOT NULL UNIQUE,
    response_data TEXT NOT NULL,
    cached_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS search_history (
    id BIGSERIAL PRIMARY KEY,
    search_term VARCHAR(100) NOT NULL,
    searched_at TIMESTAMP NOT NULL,
    was_cache_hit BOOLEAN NOT NULL DEFAULT FALSE,
    success BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_pokemon_cache_name ON pokemon_cache(pokemon_name);
CREATE INDEX IF NOT EXISTS idx_pokemon_cache_expires ON pokemon_cache(expires_at);
CREATE INDEX IF NOT EXISTS idx_search_history_term ON search_history(search_term);
CREATE INDEX IF NOT EXISTS idx_search_history_at ON search_history(searched_at DESC);

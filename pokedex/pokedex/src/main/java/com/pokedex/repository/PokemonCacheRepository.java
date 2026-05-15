package com.pokedex.repository;

import com.pokedex.entity.PokemonCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PokemonCacheRepository extends JpaRepository<PokemonCache, Long> {

    Optional<PokemonCache> findByPokemonName(String pokemonName);
}

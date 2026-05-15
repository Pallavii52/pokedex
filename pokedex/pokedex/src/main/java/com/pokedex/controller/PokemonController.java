package com.pokedex.controller;

import com.pokedex.dto.PokemonDto;
import com.pokedex.entity.SearchHistory;
import com.pokedex.service.PokemonService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/pokemon")
public class PokemonController {

    private final PokemonService pokemonService;

    public PokemonController(PokemonService pokemonService) {
        this.pokemonService = pokemonService;
    }

    @GetMapping("/{name}")
    public ResponseEntity<PokemonDto> getPokemon(@PathVariable String name) {
        PokemonDto pokemon = pokemonService.getPokemon(name);
        return ResponseEntity.ok(pokemon);
    }

    @GetMapping("/history/recent")
    public ResponseEntity<List<SearchHistory>> getRecentSearches() {
        List<SearchHistory> history = pokemonService.getRecentSearches();
        return ResponseEntity.ok(history);
    }
}

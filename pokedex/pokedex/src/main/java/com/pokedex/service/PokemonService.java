package com.pokedex.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pokedex.client.PokeApiClient;
import com.pokedex.dto.PokemonDto;
import com.pokedex.dto.StatDto;
import com.pokedex.entity.PokemonCache;
import com.pokedex.entity.SearchHistory;
import com.pokedex.repository.PokemonCacheRepository;
import com.pokedex.repository.SearchHistoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PokemonService {

    private final PokeApiClient pokeApiClient;
    private final PokemonCacheRepository pokemonCacheRepository;
    private final SearchHistoryRepository searchHistoryRepository;
    private final ObjectMapper objectMapper;

    @Value("${cache.ttl-minutes:60}")
    private int cacheTtlMinutes;

    private final Map<String, PokemonDto> memoryCache = new HashMap<>();

    public PokemonService(
            PokeApiClient pokeApiClient,
            PokemonCacheRepository pokemonCacheRepository,
            SearchHistoryRepository searchHistoryRepository,
            ObjectMapper objectMapper) {
        this.pokeApiClient = pokeApiClient;
        this.pokemonCacheRepository = pokemonCacheRepository;
        this.searchHistoryRepository = searchHistoryRepository;
        this.objectMapper = objectMapper;
    }

    public PokemonDto getPokemon(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Pokemon name cannot be empty.");
        }

        String pokemonName = name.trim().toLowerCase();

        if (memoryCache.containsKey(pokemonName)) {
            PokemonDto cached = memoryCache.get(pokemonName);
            cached.setFromCache(true);
            saveSearchHistory(pokemonName);
            return cached;
        }

        Optional<PokemonCache> dbEntry = pokemonCacheRepository.findByPokemonName(pokemonName);
        if (dbEntry.isPresent() && dbEntry.get().getExpiresAt().isAfter(LocalDateTime.now())) {
            PokemonDto dto = deserialize(dbEntry.get().getResponseData());
            dto.setFromCache(true);
            memoryCache.put(pokemonName, dto);
            saveSearchHistory(pokemonName);
            return dto;
        }

        JsonNode apiData = pokeApiClient.getPokemon(pokemonName);
        PokemonDto dto = buildPokemonDto(apiData);
        dto.setFromCache(false);

        memoryCache.put(pokemonName, dto);
        saveToDatabase(pokemonName, dto);
        saveSearchHistory(pokemonName);

        return dto;
    }

    public List<SearchHistory> getRecentSearches() {
        return searchHistoryRepository.findTop10ByOrderBySearchedAtDesc();
    }

    private PokemonDto buildPokemonDto(JsonNode data) {
        PokemonDto dto = new PokemonDto();

        dto.setId(data.path("id").asInt());
        dto.setName(data.path("name").asText());
        dto.setHeight(data.path("height").asInt());
        dto.setWeight(data.path("weight").asInt());

        String imageUrl = data.path("sprites").path("other").path("official-artwork").path("front_default").asText("");
        if (imageUrl.isEmpty()) {
            imageUrl = data.path("sprites").path("front_default").asText("");
        }
        dto.setImageUrl(imageUrl);

        String shinyUrl = data.path("sprites").path("other").path("official-artwork").path("front_shiny").asText("");
        dto.setShinyImageUrl(shinyUrl);

        List<String> types = new ArrayList<>();
        for (JsonNode typeNode : data.path("types")) {
            types.add(typeNode.path("type").path("name").asText());
        }
        dto.setTypes(types);

        List<String> abilities = new ArrayList<>();
        for (JsonNode abilityNode : data.path("abilities")) {
            abilities.add(abilityNode.path("ability").path("name").asText());
        }
        dto.setAbilities(abilities);

        List<StatDto> stats = new ArrayList<>();
        for (JsonNode statNode : data.path("stats")) {
            String statName = statNode.path("stat").path("name").asText();
            int statValue = statNode.path("base_stat").asInt();
            stats.add(new StatDto(statName, statValue));
        }
        dto.setStats(stats);

        return dto;
    }

    private void saveToDatabase(String name, PokemonDto dto) {
        try {
            String json = objectMapper.writeValueAsString(dto);
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiry = now.plusMinutes(cacheTtlMinutes);

            Optional<PokemonCache> existing = pokemonCacheRepository.findByPokemonName(name);
            if (existing.isPresent()) {
                PokemonCache entry = existing.get();
                entry.setResponseData(json);
                entry.setCachedAt(now);
                entry.setExpiresAt(expiry);
                pokemonCacheRepository.save(entry);
            } else {
                PokemonCache entry = new PokemonCache();
                entry.setPokemonName(name);
                entry.setResponseData(json);
                entry.setCachedAt(now);
                entry.setExpiresAt(expiry);
                pokemonCacheRepository.save(entry);
            }
        } catch (Exception ex) {
            System.out.println("Warning: Could not save to DB cache. " + ex.getMessage());
        }
    }

    private PokemonDto deserialize(String json) {
        try {
            return objectMapper.readValue(json, PokemonDto.class);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to read cached data.");
        }
    }

    private void saveSearchHistory(String name) {
        SearchHistory history = new SearchHistory();
        history.setSearchTerm(name);
        history.setSearchedAt(LocalDateTime.now());
        searchHistoryRepository.save(history);
    }
}

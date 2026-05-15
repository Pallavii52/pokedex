package com.pokedex.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.pokedex.exception.ExternalApiException;
import com.pokedex.exception.PokemonNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
public class PokeApiClient {

    private final RestTemplate restTemplate;

    @Value("${pokeapi.base-url}")
    private String baseUrl;

    public PokeApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public JsonNode getPokemon(String name) {
        String url = baseUrl + "/pokemon/" + name;
        try {
            return restTemplate.getForObject(url, JsonNode.class);
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new PokemonNotFoundException(name);
            }
            throw new ExternalApiException("PokeAPI error: " + ex.getStatusCode());
        } catch (Exception ex) {
            throw new ExternalApiException("Could not reach PokeAPI. Check your internet connection.");
        }
    }
}

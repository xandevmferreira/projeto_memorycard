package com.memorycard.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.memorycard.config.RawgProperties;
import com.memorycard.dto.response.ExternalGameInfo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@ConditionalOnProperty(name = "app.rawg.enabled", havingValue = "true")
public class RawgGameApiService implements ExternalGameApiService {

    private final RestClient restClient;
    private final RawgProperties rawgProperties;

    public RawgGameApiService(RestClient restClient, RawgProperties rawgProperties) {
        this.restClient = restClient;
        this.rawgProperties = rawgProperties;
    }

    @Override
    public Optional<ExternalGameInfo> searchByTitle(String title) {
        List<ExternalGameInfo> results = searchGames(title, 1);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    @Override
    public List<ExternalGameInfo> searchGames(String query, int limit) {
        try {
            JsonNode response = restClient.get()
                    .uri(rawgProperties.getBaseUrl() + "/games?key={key}&search={query}&page_size={limit}",
                            rawgProperties.getApiKey(), query, limit)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null || !response.has("results")) {
                return List.of();
            }

            List<ExternalGameInfo> games = new ArrayList<>();
            for (JsonNode node : response.get("results")) {
                games.add(mapToExternalGameInfo(node));
            }
            return games;
        } catch (Exception e) {
            return List.of();
        }
    }

    private ExternalGameInfo mapToExternalGameInfo(JsonNode node) {
        String title = node.path("name").asText();
        String coverUrl = node.path("background_image").asText(null);
        LocalDate releaseDate = parseDate(node.path("released").asText(null));
        BigDecimal rating = node.has("rating") && !node.get("rating").isNull()
                ? BigDecimal.valueOf(node.get("rating").asDouble()).setScale(1, RoundingMode.HALF_UP)
                : null;

        List<String> platforms = new ArrayList<>();
        if (node.has("platforms")) {
            for (JsonNode platformNode : node.get("platforms")) {
                platforms.add(platformNode.path("platform").path("name").asText());
            }
        }

        String platform = platforms.isEmpty() ? null : platforms.getFirst();

        return new ExternalGameInfo(title, platform, rating, coverUrl, releaseDate, platforms);
    }

    private LocalDate parseDate(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(date);
        } catch (Exception e) {
            return null;
        }
    }
}

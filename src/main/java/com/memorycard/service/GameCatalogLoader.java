package com.memorycard.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memorycard.dto.response.ExternalGameInfo;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class GameCatalogLoader {

    private final ObjectMapper objectMapper;
    private List<CatalogEntry> entries = List.of();

    public GameCatalogLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void load() {
        try (InputStream input = new ClassPathResource("games/catalog.json").getInputStream()) {
            JsonNode root = objectMapper.readTree(input);
            List<CatalogEntry> loaded = new ArrayList<>();
            for (JsonNode node : root) {
                loaded.add(parseEntry(node));
            }
            entries = List.copyOf(loaded);
        } catch (Exception e) {
            entries = List.of();
        }
    }

    public List<ExternalGameInfo> search(String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String normalized = query.toLowerCase(Locale.ROOT).trim();
        Map<String, CatalogEntry> unique = new LinkedHashMap<>();
        entries.stream()
                .filter(entry -> entry.matches(normalized))
                .sorted(Comparator.comparingInt(entry -> entry.score(normalized)))
                .forEach(entry -> unique.putIfAbsent(entry.title().toLowerCase(Locale.ROOT), entry));
        return unique.values().stream()
                .limit(limit)
                .map(CatalogEntry::toExternalGameInfo)
                .toList();
    }

    public List<ExternalGameInfo> all() {
        return entries.stream().map(CatalogEntry::toExternalGameInfo).toList();
    }

    private CatalogEntry parseEntry(JsonNode node) {
        List<String> aliases = new ArrayList<>();
        if (node.has("aliases")) {
            for (JsonNode alias : node.get("aliases")) {
                aliases.add(alias.asText().toLowerCase(Locale.ROOT));
            }
        }
        String releaseDate = node.path("releaseDate").asText(null);
        return new CatalogEntry(
                node.path("title").asText(),
                node.path("platform").asText("PC"),
                BigDecimal.valueOf(node.path("rating").asDouble(0)),
                node.path("steamAppId").asText(""),
                node.path("coverUrl").asText(null),
                releaseDate != null ? LocalDate.parse(releaseDate) : null,
                aliases
        );
    }

    private record CatalogEntry(
            String title,
            String platform,
            BigDecimal rating,
            String steamAppId,
            String customCoverUrl,
            LocalDate releaseDate,
            List<String> aliases
    ) {
        boolean matches(String query) {
            return score(query) < Integer.MAX_VALUE;
        }

        int score(String query) {
            String titleLower = title.toLowerCase(Locale.ROOT);
            if (titleLower.equals(query)) {
                return 0;
            }
            if (titleLower.startsWith(query)) {
                return 1;
            }
            if (titleLower.contains(query)) {
                return 2;
            }
            for (String alias : aliases) {
                if (alias.equals(query)) {
                    return 3;
                }
                if (alias.startsWith(query) || query.startsWith(alias)) {
                    return 4;
                }
                if (alias.contains(query)) {
                    return 5;
                }
            }
            for (String word : query.split("\\s+")) {
                if (word.length() >= 2 && titleLower.contains(word)) {
                    return 6;
                }
            }
            return Integer.MAX_VALUE;
        }

        ExternalGameInfo toExternalGameInfo() {
            String cover = resolveCoverUrl();
            return new ExternalGameInfo(
                    title,
                    platform,
                    rating,
                    cover,
                    releaseDate,
                    List.of(platform)
            );
        }

        String resolveCoverUrl() {
            if (customCoverUrl != null && !customCoverUrl.isBlank()) {
                return customCoverUrl;
            }
            if (steamAppId != null && !steamAppId.isBlank()) {
                return "https://cdn.cloudflare.steamstatic.com/steam/apps/" + steamAppId + "/library_600x900.jpg";
            }
            return null;
        }
    }
}

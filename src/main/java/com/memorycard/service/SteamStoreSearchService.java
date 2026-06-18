package com.memorycard.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.memorycard.dto.response.ExternalGameInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class SteamStoreSearchService {

    private static final Logger log = LoggerFactory.getLogger(SteamStoreSearchService.class);
    private static final String SEARCH_URL = "https://store.steampowered.com/api/storesearch/";
    private static final Pattern SKIP_NAME = Pattern.compile(
            "(?i)(dlc|soundtrack|\\bpack\\b|edition|upgrade|season pass|pass\\b|bundle|demo\\b|beta\\b|playtest| - key$|wallpaper|ost\\b|coin set|currency)",
            Pattern.CASE_INSENSITIVE
    );

    private final RestClient restClient;

    public SteamStoreSearchService(RestClient restClient) {
        this.restClient = restClient;
    }

    public List<ExternalGameInfo> search(String query, int limit) {
        if (query == null || query.isBlank() || limit <= 0) {
            return List.of();
        }
        try {
            JsonNode root = restClient.get()
                    .uri(SEARCH_URL + "?term={term}&l=english&cc=US", query.trim())
                    .header("User-Agent", "MemoryCard/1.0")
                    .retrieve()
                    .body(JsonNode.class);

            if (root == null || !root.has("items")) {
                return List.of();
            }

            List<ExternalGameInfo> results = new ArrayList<>();
            for (JsonNode item : root.get("items")) {
                if (!"app".equals(item.path("type").asText())) {
                    continue;
                }
                String name = item.path("name").asText(null);
                if (name == null || name.isBlank() || SKIP_NAME.matcher(name).find()) {
                    continue;
                }
                int appId = item.path("id").asInt(0);
                if (appId <= 0) {
                    continue;
                }

                BigDecimal metacritic = parseMetascore(item.path("metascore").asText(null));
                String platform = resolvePlatform(item.path("platforms"));
                String cover = "https://cdn.cloudflare.steamstatic.com/steam/apps/" + appId + "/library_600x900.jpg";

                results.add(new ExternalGameInfo(
                        name,
                        platform,
                        metacritic,
                        metacritic != null ? "METACRITIC" : "STEAM",
                        cover,
                        null,
                        List.of(platform)
                ));

                if (results.size() >= limit) {
                    break;
                }
            }
            return results;
        } catch (Exception e) {
            log.debug("Steam search failed for '{}': {}", query, e.getMessage());
            return List.of();
        }
    }

    private BigDecimal parseMetascore(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            int score = Integer.parseInt(value.trim());
            if (score > 0 && score <= 100) {
                return BigDecimal.valueOf(score);
            }
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    private String resolvePlatform(JsonNode platforms) {
        if (platforms == null || platforms.isMissingNode()) {
            return "PC";
        }
        List<String> parts = new ArrayList<>();
        if (platforms.path("windows").asBoolean(false)) {
            parts.add("PC");
        }
        if (platforms.path("mac").asBoolean(false)) {
            parts.add("Mac");
        }
        if (platforms.path("linux").asBoolean(false)) {
            parts.add("Linux");
        }
        return parts.isEmpty() ? "PC" : String.join(", ", parts);
    }

    public static String normalizeTitle(String title) {
        return title == null ? "" : title.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "").trim();
    }
}

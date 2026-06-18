package com.memorycard.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memorycard.config.RetroAchievementsProperties;
import com.memorycard.dto.response.RetroAchievementProgress;
import com.memorycard.dto.response.RetroGameSearchResult;
import com.memorycard.entity.User;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RetroAchievementsService {

    private static final Logger log = LoggerFactory.getLogger(RetroAchievementsService.class);
    private static final Pattern PERCENT_PATTERN = Pattern.compile("([0-9.]+)%");

    private final RestClient restClient;
    private final RetroAchievementsProperties properties;
    private final ObjectMapper objectMapper;
    private List<RetroCatalogEntry> catalog = List.of();

    public RetroAchievementsService(RestClient restClient,
                                    RetroAchievementsProperties properties,
                                    ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void loadCatalog() {
        try (InputStream input = new ClassPathResource("games/retro-catalog.json").getInputStream()) {
            JsonNode root = objectMapper.readTree(input);
            List<RetroCatalogEntry> loaded = new ArrayList<>();
            for (JsonNode node : root) {
                List<String> aliases = new ArrayList<>();
                if (node.has("aliases")) {
                    node.get("aliases").forEach(a -> aliases.add(a.asText().toLowerCase(Locale.ROOT)));
                }
                loaded.add(new RetroCatalogEntry(
                        node.path("title").asText(),
                        node.path("raGameId").asInt(),
                        node.path("consoleId").asInt(),
                        node.path("consoleName").asText(),
                        aliases
                ));
            }
            catalog = List.copyOf(loaded);
        } catch (Exception e) {
            catalog = List.of();
        }
    }

    public List<RetroGameSearchResult> search(String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String normalized = query.toLowerCase(Locale.ROOT).trim();
        return catalog.stream()
                .filter(entry -> entry.matches(normalized))
                .limit(limit)
                .map(entry -> new RetroGameSearchResult(
                        entry.title(), entry.raGameId(), entry.consoleId(), entry.consoleName()))
                .toList();
    }

    public RetroAchievementProgress fetchProgress(User user, int gameId) {
        String apiKey = resolveApiKey(user);
        String username = user.getRetroAchievementsUsername();

        if (!properties.enabled() || apiKey == null || apiKey.isBlank() || username == null || username.isBlank()) {
            return null;
        }

        try {
            String url = properties.baseUrl() + "/API/API_GetGameInfoAndUserProgress.php"
                    + "?y=" + apiKey + "&u=" + username + "&g=" + gameId;

            String body = restClient.get()
                    .uri(url)
                    .header("User-Agent", "MemoryCard/1.0")
                    .retrieve()
                    .body(String.class);

            if (body == null || body.isBlank() || body.trim().startsWith("[]")) {
                return null;
            }

            JsonNode node = objectMapper.readTree(body);
            BigDecimal percent = parsePercent(node.path("UserCompletionProgress").asText(null));
            int earned = node.path("NumAwardedToUser").asInt(0);
            int total = node.path("NumAchievements").asInt(0);
            String title = node.path("Title").asText("Jogo retro");
            String console = node.path("ConsoleName").asText("");

            return new RetroAchievementProgress(
                    gameId,
                    title,
                    console,
                    percent,
                    earned,
                    total,
                    "https://retroachievements.org/game/" + gameId,
                    true
            );
        } catch (Exception e) {
            log.debug("Falha ao buscar progresso RA: {}", e.getMessage());
            return null;
        }
    }

    public BigDecimal syncProgressToGame(User user, int gameId) {
        RetroAchievementProgress progress = fetchProgress(user, gameId);
        if (progress == null) {
            return null;
        }
        return progress.progressPercent();
    }

    private String resolveApiKey(User user) {
        if (user.getRetroAchievementsApiKey() != null && !user.getRetroAchievementsApiKey().isBlank()) {
            return user.getRetroAchievementsApiKey();
        }
        return properties.apiKey();
    }

    private BigDecimal parsePercent(String text) {
        if (text == null) {
            return BigDecimal.ZERO;
        }
        Matcher matcher = PERCENT_PATTERN.matcher(text);
        if (matcher.find()) {
            return new BigDecimal(matcher.group(1)).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    private record RetroCatalogEntry(String title, int raGameId, int consoleId, String consoleName, List<String> aliases) {
        boolean matches(String query) {
            String titleLower = title.toLowerCase(Locale.ROOT);
            if (titleLower.contains(query)) {
                return true;
            }
            for (String alias : aliases) {
                if (alias.contains(query)) {
                    return true;
                }
            }
            return false;
        }
    }
}

package com.memorycard.service;

import com.memorycard.dto.response.ExternalGameInfo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Primary
@ConditionalOnProperty(name = "app.rawg.enabled", havingValue = "false", matchIfMissing = true)
public class HybridExternalGameApiService implements ExternalGameApiService {

    private final GameCatalogLoader catalogLoader;
    private final SteamStoreSearchService steamStoreSearchService;

    public HybridExternalGameApiService(GameCatalogLoader catalogLoader,
                                        SteamStoreSearchService steamStoreSearchService) {
        this.catalogLoader = catalogLoader;
        this.steamStoreSearchService = steamStoreSearchService;
    }

    @Override
    public Optional<ExternalGameInfo> searchByTitle(String title) {
        return searchGames(title, 1).stream().findFirst();
    }

    @Override
    public List<ExternalGameInfo> searchGames(String query, int limit) {
        if (query == null || query.isBlank() || limit <= 0) {
            return List.of();
        }

        Map<String, ExternalGameInfo> merged = new LinkedHashMap<>();

        for (ExternalGameInfo game : catalogLoader.search(query, limit)) {
            merged.putIfAbsent(key(game), game);
        }

        if (merged.size() < limit) {
            int remaining = limit - merged.size();
            for (ExternalGameInfo game : steamStoreSearchService.search(query, remaining + 8)) {
                merged.putIfAbsent(key(game), game);
                if (merged.size() >= limit) {
                    break;
                }
            }
        }

        if (merged.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(merged.values()).subList(0, Math.min(limit, merged.size()));
    }

    private String key(ExternalGameInfo game) {
        return SteamStoreSearchService.normalizeTitle(game.title());
    }
}

package com.memorycard.service;

import com.memorycard.dto.response.ExternalGameInfo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@ConditionalOnProperty(name = "app.rawg.enabled", havingValue = "false", matchIfMissing = true)
public class MockExternalGameApiService implements ExternalGameApiService {

    private final GameCatalogLoader catalogLoader;

    public MockExternalGameApiService(GameCatalogLoader catalogLoader) {
        this.catalogLoader = catalogLoader;
    }

    @Override
    public Optional<ExternalGameInfo> searchByTitle(String title) {
        return searchGames(title, 1).stream().findFirst();
    }

    @Override
    public List<ExternalGameInfo> searchGames(String query, int limit) {
        return catalogLoader.search(query, limit);
    }
}

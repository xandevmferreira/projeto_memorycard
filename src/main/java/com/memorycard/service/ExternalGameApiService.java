package com.memorycard.service;

import com.memorycard.dto.response.ExternalGameInfo;

import java.util.List;
import java.util.Optional;

public interface ExternalGameApiService {

    Optional<ExternalGameInfo> searchByTitle(String title);

    List<ExternalGameInfo> searchGames(String query, int limit);
}

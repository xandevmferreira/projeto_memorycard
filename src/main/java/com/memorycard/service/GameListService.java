package com.memorycard.service;

import com.memorycard.dto.response.GameListResponse;
import com.memorycard.dto.response.GameResponse;
import com.memorycard.entity.GameList;
import com.memorycard.entity.GameListItem;
import com.memorycard.exception.ResourceNotFoundException;
import com.memorycard.repository.GameListItemRepository;
import com.memorycard.repository.GameListRepository;
import com.memorycard.repository.GameRepository;
import com.memorycard.storage.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class GameListService {

    private final GameListRepository listRepository;
    private final GameListItemRepository itemRepository;
    private final GameRepository gameRepository;
    private final StorageService storageService;
    private final CoverImageService coverImageService;

    public GameListService(GameListRepository listRepository,
                           GameListItemRepository itemRepository,
                           GameRepository gameRepository,
                           StorageService storageService,
                           CoverImageService coverImageService) {
        this.listRepository = listRepository;
        this.itemRepository = itemRepository;
        this.gameRepository = gameRepository;
        this.storageService = storageService;
        this.coverImageService = coverImageService;
    }

    @Transactional(readOnly = true)
    public List<GameListResponse> findAllByUser(Long userId) {
        return listRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(list -> toSummary(list, userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public GameListResponse findById(Long userId, Long listId) {
        GameList list = getOwnedList(userId, listId);
        return toDetail(list, userId);
    }

    @Transactional
    public GameListResponse create(Long userId, String name, String description) {
        GameList list = new GameList();
        list.setUserId(userId);
        list.setName(name.trim());
        list.setDescription(description != null ? description.trim() : null);
        list = listRepository.save(list);
        return toSummary(list, userId);
    }

    @Transactional
    public void delete(Long userId, Long listId) {
        GameList list = getOwnedList(userId, listId);
        listRepository.delete(list);
    }

    @Transactional
    public void addGame(Long userId, Long listId, Long gameId) {
        getOwnedList(userId, listId);
        gameRepository.findByIdAndUserId(gameId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Jogo não encontrado"));
        if (itemRepository.existsByListIdAndGameId(listId, gameId)) {
            return;
        }
        GameListItem item = new GameListItem();
        item.setListId(listId);
        item.setGameId(gameId);
        item.setPosition(itemRepository.countByListId(listId));
        itemRepository.save(item);
    }

    @Transactional
    public void removeGame(Long userId, Long listId, Long gameId) {
        getOwnedList(userId, listId);
        itemRepository.findByListIdAndGameId(listId, gameId)
                .ifPresent(itemRepository::delete);
    }

    private GameList getOwnedList(Long userId, Long listId) {
        return listRepository.findByIdAndUserId(listId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Lista não encontrada"));
    }

    private GameListResponse toSummary(GameList list, Long userId) {
        return new GameListResponse(
                list.getId(),
                list.getName(),
                list.getDescription(),
                itemRepository.countByListId(list.getId()),
                list.getCreatedAt(),
                List.of()
        );
    }

    private GameListResponse toDetail(GameList list, Long userId) {
        List<GameResponse> games = new ArrayList<>();
        for (GameListItem item : itemRepository.findByListIdOrderByPositionAsc(list.getId())) {
            gameRepository.findByIdAndUserId(item.getGameId(), userId).ifPresent(game ->
                    games.add(GameMapper.toResponse(game, storageService, coverImageService)));
        }
        return new GameListResponse(
                list.getId(),
                list.getName(),
                list.getDescription(),
                games.size(),
                list.getCreatedAt(),
                games
        );
    }
}

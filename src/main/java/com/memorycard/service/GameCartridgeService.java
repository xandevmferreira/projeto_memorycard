package com.memorycard.service;

import com.memorycard.dto.response.ArchiveFileView;
import com.memorycard.dto.response.CartridgeInsertView;
import com.memorycard.dto.response.CartridgeView;
import com.memorycard.dto.response.PlayerManifestView;
import com.memorycard.dto.response.RestoreManifestView;
import com.memorycard.entity.ArchiveFileType;
import com.memorycard.entity.Game;
import com.memorycard.entity.GameArchiveFile;
import com.memorycard.entity.GameCartridge;
import com.memorycard.exception.ResourceNotFoundException;
import com.memorycard.repository.GameArchiveFileRepository;
import com.memorycard.repository.GameCartridgeRepository;
import com.memorycard.repository.GameRepository;
import com.memorycard.storage.StorageService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class GameCartridgeService {

    private static final Set<String> SAVE_EXTENSIONS = Set.of(
            ".sav", ".srm", ".sra", ".sta", ".dsv", ".mcr", ".mem", ".eep", ".oops", ".ss0", ".ss1", ".ss2",
            ".save", ".dat", ".json", ".bin", ".xml", ".ini", ".cfg", ".profile"
    );
    private static final Set<String> ROM_EXTENSIONS = Set.of(
            ".sfc", ".smc", ".nes", ".fds", ".gb", ".gbc", ".gba", ".n64", ".z64", ".v64",
            ".md", ".gen", ".sms", ".gg", ".pce", ".bin", ".iso", ".cue", ".zip", ".7z"
    );
    private static final Set<String> STATE_EXTENSIONS = Set.of(
            ".state", ".st0", ".st1", ".st2", ".savestate", ".ss0", ".ss1", ".ss2", ".oops"
    );

    private static final long MAX_SAVE_BYTES = 64L * 1024 * 1024;
    private static final long MAX_ROM_BYTES = 32L * 1024 * 1024;
    private static final long MAX_STATE_BYTES = 16L * 1024 * 1024;

    private static final Set<String> IMAGE_EXTENSIONS = Set.of(".png", ".jpg", ".jpeg", ".webp", ".gif");

    private final GameRepository gameRepository;
    private final GameCartridgeRepository cartridgeRepository;
    private final GameArchiveFileRepository archiveFileRepository;
    private final StorageService storageService;
    private final CoverImageService coverImageService;

    public GameCartridgeService(GameRepository gameRepository,
                                GameCartridgeRepository cartridgeRepository,
                                GameArchiveFileRepository archiveFileRepository,
                                StorageService storageService,
                                CoverImageService coverImageService) {
        this.gameRepository = gameRepository;
        this.cartridgeRepository = cartridgeRepository;
        this.archiveFileRepository = archiveFileRepository;
        this.storageService = storageService;
        this.coverImageService = coverImageService;
    }

    @Transactional(readOnly = true)
    public List<CartridgeView> listForGame(Long userId, Long gameId) {
        getOwnedGame(userId, gameId);
        return cartridgeRepository.findByGameIdOrderByCreatedAtDesc(gameId).stream()
                .map(this::toView)
                .toList();
    }

    @Transactional
    public CartridgeView create(Long userId, Long gameId, String label, String memories,
                                LocalDate sessionDate, String emulatorHint) {
        Game game = getOwnedGame(userId, gameId);
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Informe um nome para a fita");
        }

        GameCartridge cartridge = new GameCartridge();
        cartridge.setGameId(gameId);
        cartridge.setLabel(label.trim());
        cartridge.setMemories(blankToNull(memories));
        cartridge.setSessionDate(sessionDate);
        cartridge.setEmulatorHint(blankToNull(emulatorHint));
        cartridge = cartridgeRepository.save(cartridge);
        return toView(cartridge);
    }

    @Transactional
    public CartridgeView syncUpload(Long userId, Long gameId, MultipartFile file, String label, String relativePath) {
        getOwnedGame(userId, gameId);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo vazio");
        }
        ArchiveFileType fileType = detectFileType(file.getOriginalFilename());
        if (fileType == null) {
            // Saves Xbox / jogos modernos: sem extensão ou .dat/.json na pasta de save
            String ext = extension(file.getOriginalFilename());
            if (ext.isEmpty() || SAVE_EXTENSIONS.contains(ext)) {
                fileType = ArchiveFileType.SAVE;
            } else {
                throw new IllegalArgumentException("Tipo de arquivo não reconhecido (save, ROM ou estado): " + ext);
            }
        }

        String cartridgeLabel = label != null && !label.isBlank()
                ? label.trim()
                : "Sync automático — " + LocalDate.now();
        GameCartridge cartridge = cartridgeRepository.findByGameIdOrderByCreatedAtDesc(gameId).stream()
                .filter(c -> cartridgeLabel.equals(c.getLabel()))
                .findFirst()
                .orElseGet(() -> {
                    GameCartridge created = new GameCartridge();
                    created.setGameId(gameId);
                    created.setLabel(cartridgeLabel);
                    created.setMemories("Enviado pelo sync automático");
                    created.setSessionDate(LocalDate.now());
                    return cartridgeRepository.save(created);
                });

        addFile(userId, gameId, cartridge.getId(), fileType, file, relativePath);
        return toView(cartridgeRepository.findById(cartridge.getId()).orElseThrow());
    }

    /** Grava uma fita completa: metadados + saves + screenshot em um único passo. */
    @Transactional
    public CartridgeView recordTape(Long userId, Long gameId, String label, String memories,
                                    LocalDate sessionDate, String emulatorHint,
                                    MultipartFile saveFile, MultipartFile stateFile,
                                    MultipartFile screenshot, MultipartFile[] extraFiles) {
        Game game = getOwnedGame(userId, gameId);
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Dê um nome para esta fita");
        }
        boolean hasUpload = isPresent(saveFile) || isPresent(stateFile) || isPresent(screenshot)
                || (extraFiles != null && extraFiles.length > 0);
        if (!hasUpload) {
            throw new IllegalArgumentException("Anexe pelo menos um save, estado do emulador ou screenshot");
        }

        GameCartridge cartridge = new GameCartridge();
        cartridge.setGameId(gameId);
        cartridge.setLabel(label.trim());
        cartridge.setMemories(blankToNull(memories));
        cartridge.setSessionDate(sessionDate != null ? sessionDate : LocalDate.now());
        cartridge.setEmulatorHint(blankToNull(emulatorHint != null ? emulatorHint : game.getEmulator()));
        cartridge = cartridgeRepository.save(cartridge);

        if (isPresent(screenshot)) {
            storeSnapshot(cartridge, screenshot);
        }

        if (isPresent(saveFile)) {
            addFile(userId, gameId, cartridge.getId(), ArchiveFileType.SAVE, saveFile, null);
        }
        if (isPresent(stateFile)) {
            addFile(userId, gameId, cartridge.getId(), ArchiveFileType.STATE, stateFile, null);
        }
        if (extraFiles != null) {
            for (MultipartFile file : extraFiles) {
                if (!isPresent(file)) {
                    continue;
                }
                attachRecordedFile(userId, gameId, cartridge, file);
            }
        }

        return toView(getCartridge(gameId, cartridge.getId()));
    }

    @Transactional(readOnly = true)
    public CartridgeInsertView getInsertView(Long userId, Long gameId, Long cartridgeId) {
        Game game = getOwnedGame(userId, gameId);
        GameCartridge cartridge = getCartridge(gameId, cartridgeId);
        List<ArchiveFileView> files = listFiles(cartridgeId);
        Optional<String> core = WebPlayerCoreMapper.coreForPlatform(game.getPlatform());
        Optional<ArchiveFileView> state = findLatest(files, ArchiveFileType.STATE);
        Optional<ArchiveFileView> save = findLatest(files, ArchiveFileType.SAVE);
        Optional<ArchiveFileView> preview = findStatePreview(files);

        String snapshotUrl = resolveSnapshotUrl(cartridge, preview);
        String coreName = core.orElse(null);

        return new CartridgeInsertView(
                gameId,
                game.getTitle(),
                coverImageService.toDisplayUrl(game.getCoverUrl()),
                game.getPlatform(),
                core.isPresent(),
                coreName,
                core.map(WebPlayerCoreMapper::romExtensionsHint).orElse(null),
                cartridgeId,
                cartridge.getLabel(),
                cartridge.getMemories(),
                cartridge.getSessionDate(),
                cartridge.getCreatedAt(),
                cartridge.getEmulatorHint(),
                snapshotUrl,
                state.map(ArchiveFileView::id).orElse(null),
                save.map(ArchiveFileView::id).orElse(null),
                preview.map(f -> filePublicUrl(gameId, cartridgeId, f.id())).orElse(snapshotUrl),
                state.isPresent(),
                save.isPresent()
        );
    }

    @Transactional(readOnly = true)
    public PlayerManifestView getPlayerManifest(Long userId, Long gameId, Long cartridgeId) {
        Game game = getOwnedGame(userId, gameId);
        getCartridge(gameId, cartridgeId);
        List<ArchiveFileView> files = listFiles(cartridgeId);
        Optional<String> core = WebPlayerCoreMapper.coreForPlatform(game.getPlatform());
        if (core.isEmpty()) {
            throw new IllegalArgumentException("Plataforma não suportada no navegador: " + game.getPlatform());
        }
        Optional<ArchiveFileView> state = findLatest(files, ArchiveFileType.STATE);
        Optional<ArchiveFileView> save = findLatest(files, ArchiveFileType.SAVE);

        return new PlayerManifestView(
                gameId,
                cartridgeId,
                game.getTitle(),
                game.getPlatform(),
                core.get(),
                WebPlayerCoreMapper.romExtensionsHint(core.get()),
                state.map(ArchiveFileView::id).orElse(null),
                save.map(ArchiveFileView::id).orElse(null),
                state.map(f -> filePublicUrl(gameId, cartridgeId, f.id())).orElse(null),
                save.map(f -> filePublicUrl(gameId, cartridgeId, f.id())).orElse(null),
                state.isPresent(),
                save.isPresent()
        );
    }

    @Transactional(readOnly = true)
    public Resource exportCartridgeZip(Long userId, Long gameId, Long cartridgeId) throws IOException {
        Game game = getOwnedGame(userId, gameId);
        GameCartridge cartridge = getCartridge(gameId, cartridgeId);
        List<GameArchiveFile> files = archiveFileRepository.findByCartridgeIdOrderByUploadedAtDesc(cartridgeId);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(buffer)) {
            zip.putNextEntry(new ZipEntry("LEIA-ME.txt"));
            zip.write(buildReadme(game, cartridge, files).getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();

            if (cartridge.getSnapshotPath() != null) {
                zip.putNextEntry(new ZipEntry("snapshot" + extensionOfPath(cartridge.getSnapshotPath())));
                zip.write(storageService.loadAsResource(cartridge.getSnapshotPath()).getContentAsByteArray());
                zip.closeEntry();
            }

            for (GameArchiveFile file : files) {
                String entryName = file.getRelativePath() != null && !file.getRelativePath().isBlank()
                        ? file.getRelativePath()
                        : file.getOriginalFilename();
                zip.putNextEntry(new ZipEntry(sanitizeZipEntry(entryName)));
                zip.write(storageService.loadAsResource(file.getFilePath()).getContentAsByteArray());
                zip.closeEntry();
            }
        }
        String filename = "fita-" + sanitizeFilename(cartridge.getLabel()) + ".zip";
        return new ByteArrayResource(buffer.toByteArray()) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }

    @Transactional(readOnly = true)
    public RestoreManifestView getRestoreManifest(Long userId, Long gameId) {
        getOwnedGame(userId, gameId);
        List<GameArchiveFile> saves = archiveFileRepository
                .findByGameIdAndFileTypeOrderByUploadedAtDesc(gameId, ArchiveFileType.SAVE);
        if (saves.isEmpty()) {
            return new RestoreManifestView(null, null, List.of());
        }
        Long cartridgeId = saves.getFirst().getCartridgeId();
        GameCartridge cartridge = cartridgeRepository.findById(cartridgeId)
                .orElseThrow(() -> new ResourceNotFoundException("Fita não encontrada"));
        List<ArchiveFileView> files = archiveFileRepository
                .findByCartridgeIdAndFileTypeOrderByUploadedAtDesc(cartridgeId, ArchiveFileType.SAVE)
                .stream()
                .map(this::toFileView)
                .toList();
        return new RestoreManifestView(cartridge.getId(), cartridge.getLabel(), files);
    }

    public ArchiveFileType detectFileType(String filename) {
        String ext = extension(filename);
        if (SAVE_EXTENSIONS.contains(ext)) {
            return ArchiveFileType.SAVE;
        }
        if (ROM_EXTENSIONS.contains(ext)) {
            return ArchiveFileType.ROM;
        }
        if (STATE_EXTENSIONS.contains(ext)) {
            return ArchiveFileType.STATE;
        }
        return null;
    }

    @Transactional
    public ArchiveFileView addFile(Long userId, Long gameId, Long cartridgeId,
                                   ArchiveFileType fileType, MultipartFile file) {
        return addFile(userId, gameId, cartridgeId, fileType, file, null);
    }

    @Transactional
    public ArchiveFileView addFile(Long userId, Long gameId, Long cartridgeId,
                                   ArchiveFileType fileType, MultipartFile file, String relativePath) {
        getOwnedGame(userId, gameId);
        GameCartridge cartridge = getCartridge(gameId, cartridgeId);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Selecione um arquivo");
        }

        validateFile(fileType, file);

        String directory = "archives/" + userId + "/" + gameId + "/" + cartridgeId;
        String path = storageService.store(file, directory);

        GameArchiveFile archiveFile = new GameArchiveFile();
        archiveFile.setCartridgeId(cartridge.getId());
        archiveFile.setGameId(gameId);
        archiveFile.setFileType(fileType);
        archiveFile.setFilePath(path);
        archiveFile.setOriginalFilename(safeFilename(file.getOriginalFilename()));
        archiveFile.setRelativePath(sanitizeRelativePath(relativePath));
        archiveFile.setFileSize(file.getSize());
        archiveFile = archiveFileRepository.save(archiveFile);

        return toFileView(archiveFile);
    }

    @Transactional(readOnly = true)
    public Resource downloadFile(Long userId, Long gameId, Long cartridgeId, Long fileId) {
        getOwnedGame(userId, gameId);
        getCartridge(gameId, cartridgeId);
        GameArchiveFile archiveFile = archiveFileRepository
                .findByIdAndGameIdAndCartridgeId(fileId, gameId, cartridgeId)
                .orElseThrow(() -> new ResourceNotFoundException("Arquivo não encontrado"));
        return storageService.loadAsResource(archiveFile.getFilePath());
    }

    @Transactional(readOnly = true)
    public String getOriginalFilename(Long userId, Long gameId, Long cartridgeId, Long fileId) {
        getOwnedGame(userId, gameId);
        GameArchiveFile archiveFile = archiveFileRepository
                .findByIdAndGameIdAndCartridgeId(fileId, gameId, cartridgeId)
                .orElseThrow(() -> new ResourceNotFoundException("Arquivo não encontrado"));
        return archiveFile.getOriginalFilename();
    }

    @Transactional
    public void deleteCartridge(Long userId, Long gameId, Long cartridgeId) {
        getOwnedGame(userId, gameId);
        GameCartridge cartridge = getCartridge(gameId, cartridgeId);
        List<GameArchiveFile> files = archiveFileRepository.findByCartridgeIdOrderByUploadedAtDesc(cartridgeId);
        for (GameArchiveFile file : files) {
            storageService.delete(file.getFilePath());
        }
        archiveFileRepository.deleteAll(files);
        if (cartridge.getSnapshotPath() != null) {
            storageService.delete(cartridge.getSnapshotPath());
        }
        cartridgeRepository.delete(cartridge);
    }

    @Transactional
    public void deleteFile(Long userId, Long gameId, Long cartridgeId, Long fileId) {
        getOwnedGame(userId, gameId);
        getCartridge(gameId, cartridgeId);
        GameArchiveFile archiveFile = archiveFileRepository
                .findByIdAndGameIdAndCartridgeId(fileId, gameId, cartridgeId)
                .orElseThrow(() -> new ResourceNotFoundException("Arquivo não encontrado"));
        storageService.delete(archiveFile.getFilePath());
        archiveFileRepository.delete(archiveFile);
    }

    @Transactional
    public void deleteAllForGame(Long gameId) {
        List<GameArchiveFile> files = archiveFileRepository.findByGameIdOrderByUploadedAtDesc(gameId);
        for (GameArchiveFile file : files) {
            storageService.delete(file.getFilePath());
        }
        archiveFileRepository.deleteAll(files);
        cartridgeRepository.findByGameIdOrderByCreatedAtDesc(gameId)
                .forEach(c -> {
                    if (c.getSnapshotPath() != null) {
                        storageService.delete(c.getSnapshotPath());
                    }
                    cartridgeRepository.delete(c);
                });
    }

    private List<ArchiveFileView> listFiles(Long cartridgeId) {
        return archiveFileRepository.findByCartridgeIdOrderByUploadedAtDesc(cartridgeId).stream()
                .map(this::toFileView)
                .toList();
    }

    private void attachRecordedFile(Long userId, Long gameId, GameCartridge cartridge, MultipartFile file) {
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".state.png") || (isImageExtension(extension(name)) && cartridge.getSnapshotPath() == null)) {
            storeSnapshot(cartridge, file);
            return;
        }
        ArchiveFileType type = detectFileType(name);
        if (type == null) {
            String ext = extension(name);
            if (ext.isEmpty() || SAVE_EXTENSIONS.contains(ext)) {
                type = ArchiveFileType.SAVE;
            } else if (isImageExtension(ext)) {
                storeSnapshot(cartridge, file);
                return;
            } else {
                throw new IllegalArgumentException("Arquivo não reconhecido: " + name);
            }
        }
        addFile(userId, gameId, cartridge.getId(), type, file, null);
    }

    private void storeSnapshot(GameCartridge cartridge, MultipartFile file) {
        validateImage(file);
        if (cartridge.getSnapshotPath() != null) {
            storageService.delete(cartridge.getSnapshotPath());
        }
        String path = storageService.store(file, "archives/snapshots/" + cartridge.getGameId() + "/" + cartridge.getId());
        cartridge.setSnapshotPath(path);
        cartridgeRepository.save(cartridge);
    }

    private void validateImage(MultipartFile file) {
        String ext = extension(file.getOriginalFilename());
        if (!isImageExtension(ext)) {
            throw new IllegalArgumentException("Screenshot deve ser PNG, JPG ou WEBP");
        }
        if (file.getSize() > 8L * 1024 * 1024) {
            throw new IllegalArgumentException("Screenshot grande demais (máx. 8 MB)");
        }
    }

    private boolean isImageExtension(String ext) {
        return ext != null && IMAGE_EXTENSIONS.contains(ext.toLowerCase(Locale.ROOT));
    }

    private Optional<ArchiveFileView> findLatest(List<ArchiveFileView> files, ArchiveFileType type) {
        return files.stream().filter(f -> f.fileType() == type).findFirst();
    }

    private Optional<ArchiveFileView> findStatePreview(List<ArchiveFileView> files) {
        return files.stream()
                .filter(f -> {
                    String name = f.originalFilename().toLowerCase(Locale.ROOT);
                    return name.endsWith(".state.png")
                            || (name.endsWith(".png") && f.fileType() == ArchiveFileType.STATE);
                })
                .findFirst();
    }

    private String resolveSnapshotUrl(GameCartridge cartridge, Optional<ArchiveFileView> preview) {
        if (cartridge.getSnapshotPath() != null) {
            return storageService.getPublicUrl(cartridge.getSnapshotPath());
        }
        return preview.flatMap(f -> archiveFileRepository.findById(f.id())
                .map(GameArchiveFile::getFilePath)
                .map(storageService::getPublicUrl)).orElse(null);
    }

    private String filePublicUrl(Long gameId, Long cartridgeId, Long fileId) {
        return "/games/" + gameId + "/cartridges/" + cartridgeId + "/files/" + fileId + "/download";
    }

    private String buildReadme(Game game, GameCartridge cartridge, List<GameArchiveFile> files) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        StringBuilder sb = new StringBuilder();
        sb.append("MemoryCard — Fita digital\n");
        sb.append("========================\n\n");
        sb.append("Jogo: ").append(game.getTitle()).append('\n');
        sb.append("Fita: ").append(cartridge.getLabel()).append('\n');
        if (cartridge.getSessionDate() != null) {
            sb.append("Sessão: ").append(cartridge.getSessionDate().format(fmt)).append('\n');
        }
        if (cartridge.getMemories() != null) {
            sb.append("\nMemórias:\n").append(cartridge.getMemories()).append('\n');
        }
        sb.append("\nArquivos neste pacote:\n");
        for (GameArchiveFile file : files) {
            String name = file.getRelativePath() != null ? file.getRelativePath() : file.getOriginalFilename();
            sb.append(" - ").append(name).append(" (").append(file.getFileType().getLabel()).append(")\n");
        }
        sb.append("""
                
                Como restaurar no RetroArch (Windows)
                -------------------------------------
                1. Abra o RetroArch e carregue a ROM deste jogo.
                2. Save in-game (.srm): copie para a pasta de saves do core, ex.:
                   C:\\RetroArch-Win64\\saves\\bsnes2014\\
                3. Save state (.state): copie para:
                   C:\\RetroArch-Win64\\states\\bsnes2014\\
                4. Reinicie o jogo ou use Load State no RetroArch.
                
                Dica: Settings → Directory → Save Files mostra a pasta exata no seu PC.
                """);
        return sb.toString();
    }

    private static String sanitizeZipEntry(String name) {
        return name.replace('\\', '/').replaceAll("^/+", "");
    }

    private static String sanitizeFilename(String label) {
        return label.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }

    private static String extensionOfPath(String path) {
        int i = path.lastIndexOf('.');
        return i < 0 ? ".png" : path.substring(i);
    }

    private static boolean isPresent(MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    private void validateFile(ArchiveFileType fileType, MultipartFile file) {
        String ext = extension(file.getOriginalFilename());
        long maxSize = switch (fileType) {
            case SAVE -> MAX_SAVE_BYTES;
            case ROM -> MAX_ROM_BYTES;
            case STATE -> MAX_STATE_BYTES;
        };
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("Arquivo grande demais para " + fileType.getLabel()
                    + " (máx. " + (maxSize / 1024 / 1024) + " MB)");
        }

        Set<String> allowed = switch (fileType) {
            case SAVE -> SAVE_EXTENSIONS;
            case ROM -> ROM_EXTENSIONS;
            case STATE -> STATE_EXTENSIONS;
        };
        if (!ext.isEmpty() && !allowed.contains(ext)) {
            throw new IllegalArgumentException("Extensão não permitida para " + fileType.getLabel() + ": " + ext);
        }
    }

    private CartridgeView toView(GameCartridge cartridge) {
        Game game = gameRepository.findById(cartridge.getGameId()).orElse(null);
        List<ArchiveFileView> files = listFiles(cartridge.getId());
        boolean hasState = files.stream().anyMatch(f -> f.fileType() == ArchiveFileType.STATE);
        boolean hasSave = files.stream().anyMatch(f -> f.fileType() == ArchiveFileType.SAVE);
        Optional<ArchiveFileView> preview = findStatePreview(files);
        String snapshotUrl = resolveSnapshotUrl(cartridge, preview);
        boolean canPlay = game != null && WebPlayerCoreMapper.coreForPlatform(game.getPlatform()).isPresent();

        return new CartridgeView(
                cartridge.getId(),
                cartridge.getLabel(),
                cartridge.getMemories(),
                cartridge.getSessionDate(),
                cartridge.getEmulatorHint(),
                cartridge.getCreatedAt(),
                snapshotUrl,
                hasState,
                hasSave,
                canPlay,
                files
        );
    }

    private ArchiveFileView toFileView(GameArchiveFile file) {
        return new ArchiveFileView(
                file.getId(),
                file.getFileType(),
                file.getOriginalFilename(),
                file.getRelativePath(),
                file.getFileSize(),
                file.getUploadedAt()
        );
    }

    private Game getOwnedGame(Long userId, Long gameId) {
        return gameRepository.findByIdAndUserId(gameId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Jogo não encontrado"));
    }

    private GameCartridge getCartridge(Long gameId, Long cartridgeId) {
        return cartridgeRepository.findById(cartridgeId)
                .filter(c -> c.getGameId().equals(gameId))
                .orElseThrow(() -> new ResourceNotFoundException("Fita não encontrada"));
    }

    private String extension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
    }

    private String safeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "arquivo";
        }
        return filename.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String sanitizeRelativePath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        String normalized = relativePath.replace('\\', '/').trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.contains("..")) {
            return null;
        }
        return normalized.isBlank() ? null : normalized;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

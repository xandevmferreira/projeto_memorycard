package com.memorycard.controller.api;

import com.memorycard.dto.response.ArchiveFileView;
import com.memorycard.dto.response.CartridgeView;
import com.memorycard.dto.response.PlayerManifestView;
import com.memorycard.dto.response.RestoreManifestView;
import com.memorycard.entity.ArchiveFileType;
import com.memorycard.security.SecurityUtils;
import com.memorycard.service.GameCartridgeService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/games/{gameId}")
public class GameCartridgeApiController {

    private final GameCartridgeService gameCartridgeService;

    public GameCartridgeApiController(GameCartridgeService gameCartridgeService) {
        this.gameCartridgeService = gameCartridgeService;
    }

    @GetMapping("/cartridges")
    public List<CartridgeView> list(@PathVariable("gameId") Long gameId) {
        return gameCartridgeService.listForGame(SecurityUtils.getCurrentUserId(), gameId);
    }

    @PostMapping("/cartridges")
    public CartridgeView create(@PathVariable("gameId") Long gameId,
                                @RequestParam("label") String label,
                                @RequestParam(value = "memories", required = false) String memories,
                                @RequestParam(value = "sessionDate", required = false) LocalDate sessionDate,
                                @RequestParam(value = "emulatorHint", required = false) String emulatorHint) {
        return gameCartridgeService.create(
                SecurityUtils.getCurrentUserId(), gameId, label, memories, sessionDate, emulatorHint);
    }

    @PostMapping(value = "/cartridges/{cartridgeId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ArchiveFileView uploadFile(@PathVariable("gameId") Long gameId,
                                      @PathVariable("cartridgeId") Long cartridgeId,
                                      @RequestParam("fileType") ArchiveFileType fileType,
                                      @RequestPart("file") MultipartFile file) {
        return gameCartridgeService.addFile(
                SecurityUtils.getCurrentUserId(), gameId, cartridgeId, fileType, file);
    }

    /** Upload automático: detecta tipo, cria/reutiliza fita do dia. Usado pelo agente opcional no PC do usuário. */
    @PostMapping(value = "/archives/sync", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CartridgeView syncUpload(@PathVariable("gameId") Long gameId,
                                    @RequestPart("file") MultipartFile file,
                                    @RequestParam(value = "label", required = false) String label,
                                    @RequestParam(value = "relativePath", required = false) String relativePath) {
        return gameCartridgeService.syncUpload(SecurityUtils.getCurrentUserId(), gameId, file, label, relativePath);
    }

    /** Lista saves mais recentes para restaurar no PC (agente automático). */
    @GetMapping("/archives/restore-manifest")
    public RestoreManifestView restoreManifest(@PathVariable("gameId") Long gameId) {
        return gameCartridgeService.getRestoreManifest(SecurityUtils.getCurrentUserId(), gameId);
    }

    @GetMapping("/cartridges/{cartridgeId}/player-manifest")
    public PlayerManifestView playerManifest(@PathVariable("gameId") Long gameId,
                                             @PathVariable("cartridgeId") Long cartridgeId) {
        return gameCartridgeService.getPlayerManifest(SecurityUtils.getCurrentUserId(), gameId, cartridgeId);
    }

    @PostMapping(value = "/cartridges/{cartridgeId}/quick-save", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ArchiveFileView quickSaveFromBrowser(@PathVariable("gameId") Long gameId,
                                                @PathVariable("cartridgeId") Long cartridgeId,
                                                @RequestPart("file") MultipartFile file,
                                                @RequestParam(value = "fileType", defaultValue = "STATE") ArchiveFileType fileType) {
        return gameCartridgeService.addFile(
                SecurityUtils.getCurrentUserId(), gameId, cartridgeId, fileType, file);
    }

    @GetMapping("/cartridges/{cartridgeId}/files/{fileId}/download")
    public ResponseEntity<Resource> download(@PathVariable("gameId") Long gameId,
                                             @PathVariable("cartridgeId") Long cartridgeId,
                                             @PathVariable("fileId") Long fileId) {
        Long userId = SecurityUtils.getCurrentUserId();
        Resource resource = gameCartridgeService.downloadFile(userId, gameId, cartridgeId, fileId);
        String filename = gameCartridgeService.getOriginalFilename(userId, gameId, cartridgeId, fileId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}

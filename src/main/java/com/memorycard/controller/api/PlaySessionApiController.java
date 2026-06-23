package com.memorycard.controller.api;

import com.memorycard.dto.response.PlaySessionView;
import com.memorycard.security.SecurityUtils;
import com.memorycard.service.PlaySessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/play-sessions")
public class PlaySessionApiController {

    private final PlaySessionService playSessionService;

    public PlaySessionApiController(PlaySessionService playSessionService) {
        this.playSessionService = playSessionService;
    }

    @GetMapping("/active")
    public ResponseEntity<PlaySessionView> active() {
        PlaySessionView session = playSessionService.findAnyActive(SecurityUtils.getCurrentUserId());
        if (session == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(session);
    }

    @PostMapping("/start")
    public ResponseEntity<PlaySessionView> start(@RequestBody Map<String, Object> body) {
        Long gameId = Long.valueOf(body.get("gameId").toString());
        String source = body.get("source") != null ? body.get("source").toString() : "TRACKER";
        String processName = body.get("processName") != null ? body.get("processName").toString() : null;
        return ResponseEntity.ok(playSessionService.startSession(
                SecurityUtils.getCurrentUserId(), gameId, source, processName));
    }

    @PostMapping("/stop")
    public ResponseEntity<PlaySessionView> stop(@RequestBody(required = false) Map<String, Object> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (body != null && body.get("gameId") != null) {
            Long gameId = Long.valueOf(body.get("gameId").toString());
            return ResponseEntity.ok(playSessionService.stopSession(userId, gameId));
        }
        return ResponseEntity.ok(playSessionService.stopAnyActive(userId));
    }
}

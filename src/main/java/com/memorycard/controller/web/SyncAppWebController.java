package com.memorycard.controller.web;

import com.memorycard.security.SecurityUtils;
import com.memorycard.service.SyncAppDownloadService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;

@Controller
public class SyncAppWebController {

    private final SyncAppDownloadService syncAppDownloadService;

    public SyncAppWebController(SyncAppDownloadService syncAppDownloadService) {
        this.syncAppDownloadService = syncAppDownloadService;
    }

    @GetMapping("/sync-app")
    public String page() {
        return "sync-app/index";
    }

    @GetMapping("/sync-app/download")
    public ResponseEntity<byte[]> download(HttpServletRequest request) throws IOException {
        Long userId = SecurityUtils.getCurrentUserId();
        String baseUrl = resolveBaseUrl(request);
        byte[] zip = syncAppDownloadService.buildDownloadZip(userId, baseUrl);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"MemoryCardSync.zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(zip);
    }

    static String resolveBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        boolean defaultPort = ("http".equals(scheme) && port == 80)
                || ("https".equals(scheme) && port == 443);
        return defaultPort ? scheme + "://" + host : scheme + "://" + host + ":" + port;
    }
}

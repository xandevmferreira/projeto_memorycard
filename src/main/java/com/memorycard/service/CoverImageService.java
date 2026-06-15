package com.memorycard.service;

import com.memorycard.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;
import java.util.Locale;

@Service
public class CoverImageService {

    private static final Logger log = LoggerFactory.getLogger(CoverImageService.class);

    private static final List<String> ALLOWED_HOSTS = List.of(
            "media.rawg.io",
            "images.rawg.io",
            "cdn.akamai.steamstatic.com",
            "cdn.cloudflare.steamstatic.com"
    );

    private final RestClient restClient;
    private final StorageService storageService;

    public CoverImageService(RestClient restClient, StorageService storageService) {
        this.restClient = restClient;
        this.storageService = storageService;
    }

    public boolean isExternalUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        return url.startsWith("http://") || url.startsWith("https://");
    }

    public boolean isAllowedUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        try {
            URI uri = URI.create(url.trim());
            String host = uri.getHost();
            return host != null && ALLOWED_HOSTS.contains(host.toLowerCase(Locale.ROOT));
        } catch (Exception e) {
            return false;
        }
    }

    public String toDisplayUrl(String coverUrl) {
        if (coverUrl == null || coverUrl.isBlank()) {
            return null;
        }
        if (coverUrl.startsWith("/")) {
            return coverUrl;
        }
        if (isAllowedUrl(coverUrl)) {
            return "/covers/proxy?url=" + java.net.URLEncoder.encode(coverUrl, java.nio.charset.StandardCharsets.UTF_8);
        }
        return coverUrl;
    }

    public String persistFromUrl(String url, Long userId) {
        if (!isAllowedUrl(url)) {
            return null;
        }

        byte[] imageBytes = fetchBytes(url);
        if (imageBytes == null || imageBytes.length == 0) {
            log.warn("Não foi possível baixar a capa: {}", url);
            return null;
        }

        String extension = resolveExtension(url);
        return storageService.storeBytes(imageBytes, "covers/" + userId, extension);
    }

    public ResponseEntity<byte[]> fetchImage(String url) {
        if (!isAllowedUrl(url)) {
            return ResponseEntity.badRequest().build();
        }

        byte[] imageBytes = fetchBytes(url);
        if (imageBytes == null || imageBytes.length == 0) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(resolveMediaType(url))
                .body(imageBytes);
    }

    private byte[] fetchBytes(String url) {
        byte[] bytes = tryFetch(url);
        if (bytes != null) {
            return bytes;
        }

        String fallback = steamFallbackUrl(url);
        if (fallback != null) {
            return tryFetch(fallback);
        }
        return null;
    }

    private byte[] tryFetch(String url) {
        try {
            byte[] body = restClient.get()
                    .uri(url)
                    .header(HttpHeaders.USER_AGENT, "Mozilla/5.0 (compatible; MemoryCard/1.0)")
                    .header(HttpHeaders.ACCEPT, "image/*")
                    .retrieve()
                    .body(byte[].class);
            if (body != null && body.length > 100) {
                return body;
            }
        } catch (Exception e) {
            log.debug("Falha ao buscar imagem {}: {}", url, e.getMessage());
        }
        return null;
    }

    private String steamFallbackUrl(String url) {
        if (url == null || !url.contains("steamstatic.com/steam/apps/")) {
            return null;
        }
        int start = url.indexOf("/steam/apps/") + "/steam/apps/".length();
        int end = url.indexOf('/', start);
        if (end < 0) {
            end = url.length();
        }
        String appId = url.substring(start, end);
        String base = "https://cdn.cloudflare.steamstatic.com/steam/apps/" + appId + "/";
        if (url.contains("header.jpg")) {
            return base + "library_600x900.jpg";
        }
        if (url.contains("library_600x900.jpg")) {
            return base + "header.jpg";
        }
        return null;
    }

    private MediaType resolveMediaType(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (lower.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        if (lower.endsWith(".gif")) {
            return MediaType.IMAGE_GIF;
        }
        return MediaType.IMAGE_JPEG;
    }

    private String resolveExtension(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.contains(".png")) {
            return ".png";
        }
        if (lower.contains(".webp")) {
            return ".webp";
        }
        return ".jpg";
    }
}

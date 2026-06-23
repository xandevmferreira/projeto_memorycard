package com.memorycard.sync.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

public class MemoryCardClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private final String baseUrl;
    private String token;

    public MemoryCardClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public void setSyncToken(String syncToken) {
        this.token = syncToken == null ? null : syncToken.trim();
    }

    /** Valida o token tentando listar jogos (token de sync ou JWT). */
    public List<GameSummary> connect() throws IOException, InterruptedException {
        if (token == null || token.isBlank()) {
            throw new IOException("Informe o token de sync (Perfil → MemoryCard Sync)");
        }
        return listGames();
    }

    public List<GameSummary> listGames() throws IOException, InterruptedException {
        HttpRequest request = authorized(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/games"))
                .GET());
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 401) {
            throw new UnauthorizedException();
        }
        if (response.statusCode() != 200) {
            throw new IOException("Erro ao listar jogos (HTTP " + response.statusCode() + ")");
        }
        return Arrays.asList(MAPPER.readValue(response.body(), GameSummary[].class));
    }

    public void syncFile(long gameId, Path file) throws IOException, InterruptedException {
        syncFile(gameId, file, null);
    }

    public void syncFile(long gameId, Path file, String relativePath) throws IOException, InterruptedException {
        byte[] fileBytes = Files.readAllBytes(file);
        String boundary = "----MemoryCardSync" + System.currentTimeMillis();
        String filename = file.getFileName().toString();

        byte[] body = relativePath != null && !relativePath.isBlank()
                ? buildMultipartWithField(boundary, "file", filename, "application/octet-stream", fileBytes, "relativePath", relativePath)
                : buildMultipart(boundary, "file", filename, "application/octet-stream", fileBytes);

        HttpRequest request = authorized(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/games/" + gameId + "/archives/sync"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body)));

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 401) {
            throw new UnauthorizedException();
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Upload falhou (HTTP " + response.statusCode() + "): " + response.body());
        }
    }

    public RestoreManifest fetchRestoreManifest(long gameId) throws IOException, InterruptedException {
        HttpRequest request = authorized(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/games/" + gameId + "/archives/restore-manifest"))
                .GET());
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 401) {
            throw new UnauthorizedException();
        }
        if (response.statusCode() != 200) {
            throw new IOException("Erro ao buscar saves (HTTP " + response.statusCode() + ")");
        }
        return MAPPER.readValue(response.body(), RestoreManifest.class);
    }

    public byte[] downloadFile(long gameId, long cartridgeId, long fileId) throws IOException, InterruptedException {
        HttpRequest request = authorized(HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/games/" + gameId + "/cartridges/" + cartridgeId + "/files/" + fileId + "/download"))
                .GET());
        HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() == 401) {
            throw new UnauthorizedException();
        }
        if (response.statusCode() != 200) {
            throw new IOException("Download falhou (HTTP " + response.statusCode() + ")");
        }
        return response.body();
    }

    private HttpRequest authorized(HttpRequest.Builder builder) {
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Token não configurado");
        }
        return builder.header("Authorization", "Bearer " + token).build();
    }

    private static byte[] buildMultipart(String boundary, String field, String filename,
                                         String contentType, byte[] content) {
        String header = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + field + "\"; filename=\"" + filename + "\"\r\n"
                + "Content-Type: " + contentType + "\r\n\r\n";
        String footer = "\r\n--" + boundary + "--\r\n";
        byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);
        byte[] footerBytes = footer.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[headerBytes.length + content.length + footerBytes.length];
        System.arraycopy(headerBytes, 0, result, 0, headerBytes.length);
        System.arraycopy(content, 0, result, headerBytes.length, content.length);
        System.arraycopy(footerBytes, 0, result, headerBytes.length + content.length, footerBytes.length);
        return result;
    }

    private static byte[] buildMultipartWithField(String boundary, String fileField, String filename,
                                                  String contentType, byte[] content,
                                                  String textField, String textValue) {
        String filePart = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + fileField + "\"; filename=\"" + filename + "\"\r\n"
                + "Content-Type: " + contentType + "\r\n\r\n";
        String textPart = "\r\n--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + textField + "\"\r\n\r\n"
                + textValue;
        String footer = "\r\n--" + boundary + "--\r\n";
        byte[] filePartBytes = filePart.getBytes(StandardCharsets.UTF_8);
        byte[] textPartBytes = textPart.getBytes(StandardCharsets.UTF_8);
        byte[] footerBytes = footer.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[filePartBytes.length + content.length + textPartBytes.length + footerBytes.length];
        int offset = 0;
        System.arraycopy(filePartBytes, 0, result, offset, filePartBytes.length);
        offset += filePartBytes.length;
        System.arraycopy(content, 0, result, offset, content.length);
        offset += content.length;
        System.arraycopy(textPartBytes, 0, result, offset, textPartBytes.length);
        offset += textPartBytes.length;
        System.arraycopy(footerBytes, 0, result, offset, footerBytes.length);
        return result;
    }

    public static class UnauthorizedException extends IOException {
        public UnauthorizedException() {
            super("Token inválido ou expirado — gere um novo em Perfil → MemoryCard Sync");
        }
    }
}

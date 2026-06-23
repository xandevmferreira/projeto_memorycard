package com.memorycard.service;

import com.memorycard.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class SyncAppDownloadService {

    private final SyncTokenService syncTokenService;
    private final UserRepository userRepository;
    private final Path devJarPath;

    public SyncAppDownloadService(SyncTokenService syncTokenService,
                                  UserRepository userRepository,
                                  @Value("${memorycard.sync-agent-dev-jar:}") String devJarPath) {
        this.syncTokenService = syncTokenService;
        this.userRepository = userRepository;
        this.devJarPath = devJarPath.isBlank() ? null : Path.of(devJarPath);
    }

    public byte[] buildDownloadZip(Long userId, String baseUrl) throws IOException {
        String token = syncTokenService.generateToken(userId);
        String email = userRepository.findById(userId)
                .map(u -> u.getEmail())
                .orElse("sua-conta");

        byte[] jarBytes = readAgentJar();

        String props = "baseUrl=" + baseUrl + "\n"
                + "syncToken=" + token + "\n"
                + "accountEmail=" + email + "\n";

        String bat = """
                @echo off
                cd /d "%~dp0"
                where java >nul 2>nul
                if errorlevel 1 (
                  echo Java nao encontrado. Instale Java 21+ de https://adoptium.net
                  pause
                  exit /b 1
                )
                start "" javaw -jar "%~dp0MemoryCard-Sync.jar"
                """;

        String batDebug = """
                @echo off
                cd /d "%~dp0"
                java -jar "%~dp0MemoryCard-Sync.jar"
                if errorlevel 1 pause
                """;

        String readme = """
                MemoryCard Sync v0.0.11

                1. Dê duplo clique em Iniciar-MemoryCard-Sync.bat
                2. Escolha o jogo e clique em DETECTAR SAVES
                3. Quando aparecer a mensagem de sucesso, clique em Iniciar sync

                Este pacote já está vinculado à sua conta no site.
                Não compartilhe esta pasta (contém chave de acesso).

                Saves aparecem no site em Fitas digitais do jogo escolhido.
                """;

        ByteArrayOutputStream zipBytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(zipBytes)) {
            addEntry(zip, "MemoryCard-Sync.jar", jarBytes);
            addEntry(zip, "connection.properties", props.getBytes(StandardCharsets.UTF_8));
            addEntry(zip, "Iniciar-MemoryCard-Sync.bat", bat.getBytes(StandardCharsets.UTF_8));
            addEntry(zip, "Iniciar-com-log.bat", batDebug.getBytes(StandardCharsets.UTF_8));
            addEntry(zip, "LEIA-ME.txt", readme.getBytes(StandardCharsets.UTF_8));
        }
        return zipBytes.toByteArray();
    }

    private byte[] readAgentJar() throws IOException {
        // Prefer JAR recém-compilado (dev) para o download sempre trazer a versão nova
        if (devJarPath != null && Files.isRegularFile(devJarPath)) {
            return Files.readAllBytes(devJarPath);
        }
        Resource classpath = new ClassPathResource("static/download/memorycard-sync.jar");
        if (classpath.exists()) {
            try (InputStream in = classpath.getInputStream()) {
                return in.readAllBytes();
            }
        }
        throw new IllegalStateException(
                "Programa de sync não disponível no servidor. Contate o administrador.");
    }

    private static void addEntry(ZipOutputStream zip, String name, byte[] data) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        zip.putNextEntry(entry);
        zip.write(data);
        zip.closeEntry();
    }
}

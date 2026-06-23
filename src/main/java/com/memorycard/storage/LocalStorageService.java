package com.memorycard.storage;

import com.memorycard.config.StorageProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private final StorageProperties storageProperties;

    public LocalStorageService(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @Override
    public String store(MultipartFile file, String directory) {
        try {
            Path basePath = Path.of(storageProperties.getLocal().getBasePath());
            Path targetDir = basePath.resolve(directory);
            Files.createDirectories(targetDir);

            String extension = getExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID() + extension;
            Path targetFile = targetDir.resolve(filename);

            Files.copy(file.getInputStream(), targetFile);
            return directory + "/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Falha ao salvar arquivo", e);
        }
    }

    @Override
    public String storeBytes(byte[] content, String directory, String extension) {
        try {
            Path basePath = Path.of(storageProperties.getLocal().getBasePath());
            Path targetDir = basePath.resolve(directory);
            Files.createDirectories(targetDir);

            String safeExtension = extension != null && extension.startsWith(".") ? extension : ".jpg";
            String filename = UUID.randomUUID() + safeExtension;
            Path targetFile = targetDir.resolve(filename);

            Files.write(targetFile, content);
            return directory + "/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Falha ao salvar arquivo", e);
        }
    }

    @Override
    public void delete(String filePath) {
        try {
            Path fullPath = Path.of(storageProperties.getLocal().getBasePath()).resolve(filePath);
            Files.deleteIfExists(fullPath);
        } catch (IOException e) {
            throw new RuntimeException("Falha ao remover arquivo", e);
        }
    }

    @Override
    public String getPublicUrl(String filePath) {
        if (filePath == null) {
            return null;
        }
        return storageProperties.getLocal().getBaseUrl() + "/" + filePath;
    }

    @Override
    public Resource loadAsResource(String filePath) {
        try {
            Path fullPath = Path.of(storageProperties.getLocal().getBasePath()).resolve(filePath).normalize();
            Path basePath = Path.of(storageProperties.getLocal().getBasePath()).normalize();
            if (!fullPath.startsWith(basePath)) {
                throw new RuntimeException("Caminho de arquivo inválido");
            }
            Resource resource = new UrlResource(fullPath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new RuntimeException("Arquivo não encontrado");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new RuntimeException("Falha ao carregar arquivo", e);
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }
}

package com.memorycard.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    String store(MultipartFile file, String directory);

    String storeBytes(byte[] content, String directory, String extension);

    void delete(String filePath);

    String getPublicUrl(String filePath);
}

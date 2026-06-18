package com.memorycard.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Implementação futura para AWS S3 ou Cloudflare R2.
 * Ative com app.storage.type=s3 ou app.storage.type=r2
 */
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
public class S3StorageService implements StorageService {

    @Override
    public String store(MultipartFile file, String directory) {
        throw new UnsupportedOperationException("Storage S3/R2 ainda não implementado. Use app.storage.type=local");
    }

    @Override
    public String storeBytes(byte[] content, String directory, String extension) {
        throw new UnsupportedOperationException("Storage S3/R2 ainda não implementado. Use app.storage.type=local");
    }

    @Override
    public void delete(String filePath) {
        throw new UnsupportedOperationException("Storage S3/R2 ainda não implementado");
    }

    @Override
    public String getPublicUrl(String filePath) {
        throw new UnsupportedOperationException("Storage S3/R2 ainda não implementado");
    }
}

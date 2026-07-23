package br.com.ecofy.ms_ingestion.core.port.out;

import java.io.InputStream;
import java.util.UUID;

public interface StoreFilePort {

    StoredFile store(UUID fileId, UUID ownerId, String originalFileName, InputStream content, long maxBytes);

    void delete(String storedPath);

    record StoredFile(String storedPath, String contentHash, long sizeBytes) {
    }
}

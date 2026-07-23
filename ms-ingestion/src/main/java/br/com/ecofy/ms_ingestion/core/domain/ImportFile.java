package br.com.ecofy.ms_ingestion.core.domain;

import br.com.ecofy.ms_ingestion.core.domain.enums.ImportFileType;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

// Representa o arquivo recebido para importação, com dono e hash de conteúdo como chave de idempotência.
public class ImportFile {

    private final UUID id;
    private final UUID userId;
    private final String originalFileName;
    private final String storedPath;
    private final ImportFileType type;
    private final long sizeBytes;
    private final String fileHash;
    private final String idempotencyKey;
    private final Instant uploadedAt;

    public ImportFile(UUID id,
                      UUID userId,
                      String originalFileName,
                      String storedPath,
                      ImportFileType type,
                      long sizeBytes,
                      String fileHash,
                      String idempotencyKey,
                      Instant uploadedAt) {

        this.id = Objects.requireNonNull(id, "id must not be null");
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.originalFileName = Objects.requireNonNull(originalFileName, "originalFileName must not be null");
        this.storedPath = Objects.requireNonNull(storedPath, "storedPath must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.sizeBytes = sizeBytes;
        this.fileHash = Objects.requireNonNull(fileHash, "fileHash must not be null");
        this.idempotencyKey = idempotencyKey;
        this.uploadedAt = Objects.requireNonNull(uploadedAt, "uploadedAt must not be null");
    }

    public static ImportFile create(UUID id,
                                    UUID userId,
                                    String originalFileName,
                                    String storedPath,
                                    ImportFileType type,
                                    long sizeBytes,
                                    String fileHash,
                                    String idempotencyKey) {

        return new ImportFile(
                id,
                userId,
                originalFileName,
                storedPath,
                type,
                sizeBytes,
                fileHash,
                idempotencyKey,
                Instant.now()
        );
    }

    public UUID id() {
        return id;
    }

    public UUID userId() {
        return userId;
    }

    public String originalFileName() {
        return originalFileName;
    }

    public String storedPath() {
        return storedPath;
    }

    public ImportFileType type() {
        return type;
    }

    public long sizeBytes() {
        return sizeBytes;
    }

    public String fileHash() {
        return fileHash;
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }

    public Instant uploadedAt() {
        return uploadedAt;
    }
}

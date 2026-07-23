package br.com.ecofy.ms_ingestion.adapters.out.storage;

import br.com.ecofy.ms_ingestion.config.IngestionProperties;
import br.com.ecofy.ms_ingestion.config.StorageProperties;
import br.com.ecofy.ms_ingestion.core.application.exception.FileTooLargeException;
import br.com.ecofy.ms_ingestion.core.application.exception.InvalidFileSizeException;
import br.com.ecofy.ms_ingestion.core.application.exception.StorageException;
import br.com.ecofy.ms_ingestion.core.port.out.StoreFilePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

// Armazena arquivos em streaming com validação de tamanho e cálculo de hash.
@Slf4j
@Component
public class LocalStorageFileAdapter implements StoreFilePort {

    private static final int BUFFER_SIZE = 8192;

    private final StorageProperties storageProperties;
    private final IngestionProperties ingestionProperties;

    public LocalStorageFileAdapter(StorageProperties storageProperties,
                                   IngestionProperties ingestionProperties) {
        this.storageProperties = Objects.requireNonNull(storageProperties, "storageProperties must not be null");
        this.ingestionProperties = Objects.requireNonNull(ingestionProperties, "ingestionProperties must not be null");
    }

    // Grava o conteúdo e remove arquivos parciais quando o processamento falha.
    @Override
    public StoredFile store(UUID fileId,
                            UUID ownerId,
                            String originalFileName,
                            InputStream content,
                            long maxBytes) {

        Objects.requireNonNull(fileId, "fileId must not be null");
        Objects.requireNonNull(ownerId, "ownerId must not be null");
        Objects.requireNonNull(content, "content must not be null");

        Path target = resolveTarget(fileId, ownerId, originalFileName);
        MessageDigest digest = newDigest();

        long total = 0;
        boolean completed = false;

        try {
            Files.createDirectories(target.getParent());

            try (OutputStream fileOut = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW);
                 DigestOutputStream out = new DigestOutputStream(fileOut, digest)) {

                byte[] buffer = new byte[BUFFER_SIZE];
                int read;
                while ((read = content.read(buffer)) != -1) {
                    total += read;
                    if (total > maxBytes) {
                        throw new FileTooLargeException(total, maxBytes);
                    }
                    out.write(buffer, 0, read);
                }
            }

            if (total == 0) {
                throw new InvalidFileSizeException(0);
            }

            completed = true;

            String hash = HexFormat.of().formatHex(digest.digest());

            log.info("[LocalStorageFileAdapter] - [store] -> Arquivo armazenado fileId={} sizeBytes={}",
                    fileId, total);

            return new StoredFile(target.toAbsolutePath().toString(), hash, total);

        } catch (IOException e) {
            log.error("[LocalStorageFileAdapter] - [store] -> Erro ao gravar fileId={} error={}",
                    fileId, e.getMessage(), e);
            throw new StorageException("Error storing file", e);
        } finally {
            if (!completed) {
                deleteQuietly(target);
            }
        }
    }

    @Override
    public void delete(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return;
        }
        deleteQuietly(Path.of(storedPath));
    }

    // Resolve um caminho seguro particionado por proprietário e data.
    private Path resolveTarget(UUID fileId, UUID ownerId, String originalFileName) {
        Path baseDir = Path.of(storageProperties.getBasePath()).toAbsolutePath().normalize();

        String dateDir = Instant.now()
                .atZone(ZoneOffset.UTC)
                .toLocalDate()
                .format(DateTimeFormatter.BASIC_ISO_DATE);

        Path dir = baseDir.resolve(ownerId.toString()).resolve(dateDir).normalize();

        String safeName = fileId + "-" + sanitizeFileName(originalFileName);
        Path target = dir.resolve(safeName).normalize();

        if (!target.startsWith(baseDir)) {
            throw new StorageException("Resolved storage path escapes base directory", null);
        }
        return target;
    }

    private MessageDigest newDigest() {
        String algorithm = ingestionProperties.getIdempotency().getAlgorithm();
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new StorageException("Hash algorithm not available: " + algorithm, e);
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("[LocalStorageFileAdapter] - [deleteQuietly] -> Falha ao remover arquivo error={}",
                    e.getMessage());
        }
    }

    // Sanitiza o nome para uso seguro no sistema de arquivos.
    static String sanitizeFileName(String original) {
        if (original == null || original.isBlank()) {
            return "upload";
        }

        String base = original.replace("\\", "/");
        int slash = base.lastIndexOf('/');
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }

        base = base.replaceAll("[^A-Za-z0-9._-]", "_");
        base = base.replaceAll("^\\.+", "");

        if (base.isBlank()) {
            return "upload";
        }

        return base.length() > 120 ? base.substring(0, 120) : base;
    }
}

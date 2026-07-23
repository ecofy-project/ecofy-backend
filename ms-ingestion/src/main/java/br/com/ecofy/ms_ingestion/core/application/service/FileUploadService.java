package br.com.ecofy.ms_ingestion.core.application.service;

import br.com.ecofy.ms_ingestion.config.IngestionProperties;
import br.com.ecofy.ms_ingestion.core.application.exception.FileTooLargeException;
import br.com.ecofy.ms_ingestion.core.application.exception.IdempotencyKeyPayloadMismatchException;
import br.com.ecofy.ms_ingestion.core.application.exception.ImportAlreadyProcessedException;
import br.com.ecofy.ms_ingestion.core.application.exception.IngestionErrorCode;
import br.com.ecofy.ms_ingestion.core.application.exception.IngestionException;
import br.com.ecofy.ms_ingestion.core.application.exception.StorageException;
import br.com.ecofy.ms_ingestion.core.application.exception.UnsupportedFileTypeException;
import br.com.ecofy.ms_ingestion.core.domain.ImportFile;
import br.com.ecofy.ms_ingestion.core.domain.ImportJob;
import br.com.ecofy.ms_ingestion.core.domain.enums.ImportFileType;
import br.com.ecofy.ms_ingestion.core.port.in.UploadFileUseCase;
import br.com.ecofy.ms_ingestion.core.port.out.FileContentLoaderPort;
import br.com.ecofy.ms_ingestion.core.port.out.LoadImportJobPort;
import br.com.ecofy.ms_ingestion.core.port.out.SaveImportFilePort;
import br.com.ecofy.ms_ingestion.core.port.out.StoreFilePort;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

// Recebe o arquivo validando metadados baratos, gravando em streaming e só então decidindo a idempotência pelo hash.
@Slf4j
@Service
public class FileUploadService implements UploadFileUseCase {

    private static final int SNIFF_BYTES = 8192;

    private final SaveImportFilePort saveImportFilePort;
    private final LoadImportJobPort loadImportJobPort;
    private final StoreFilePort storeFilePort;
    private final FileContentLoaderPort fileContentLoaderPort;
    private final IngestionProperties properties;
    private final MeterRegistry meterRegistry;

    public FileUploadService(SaveImportFilePort saveImportFilePort,
                             LoadImportJobPort loadImportJobPort,
                             StoreFilePort storeFilePort,
                             FileContentLoaderPort fileContentLoaderPort,
                             IngestionProperties properties,
                             MeterRegistry meterRegistry) {
        this.saveImportFilePort = Objects.requireNonNull(saveImportFilePort, "saveImportFilePort must not be null");
        this.loadImportJobPort = Objects.requireNonNull(loadImportJobPort, "loadImportJobPort must not be null");
        this.storeFilePort = Objects.requireNonNull(storeFilePort, "storeFilePort must not be null");
        this.fileContentLoaderPort =
                Objects.requireNonNull(fileContentLoaderPort, "fileContentLoaderPort must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry must not be null");
    }

    @Override
    public ImportFile upload(UploadFileCommand command) {
        if (command == null) {
            throw new IngestionException(IngestionErrorCode.INVALID_COMMAND, "command must not be null");
        }

        UUID ownerId = Objects.requireNonNull(command.ownerId(), "ownerId must not be null");
        ImportFileType type = command.type();
        IngestionProperties.Upload limits = properties.getUpload();

        log.info("[FileUploadService] - [upload] -> Recebendo arquivo type={} declaredSizeBytes={}",
                type, command.declaredSizeBytes());

        validateDeclaredMetadata(command, limits);

        String idempotencyKey = normalizeIdempotencyKey(command.idempotencyKey());
        UUID fileId = UUID.randomUUID();

        StoreFilePort.StoredFile stored;
        try {
            stored = storeFilePort.store(
                    fileId, ownerId, command.originalFileName(), command.content(), limits.getMaxFileSize().toBytes());
        } catch (FileTooLargeException e) {
            countRejection("size_limit");
            throw e;
        }

        // A partir daqui o arquivo existe no storage: qualquer rejeição precisa removê-lo.
        boolean keep = false;
        try {
            validateRealContent(type, stored.storedPath());

            checkIdempotency(ownerId, idempotencyKey, stored.contentHash());

            ImportFile file = ImportFile.create(
                    fileId,
                    ownerId,
                    safeOriginalName(command.originalFileName()),
                    stored.storedPath(),
                    type,
                    stored.sizeBytes(),
                    stored.contentHash(),
                    idempotencyKey
            );

            // save() traduz violação da unique constraint em ImportAlreadyProcessedException:
            // é o que resolve o upload concorrente que passou pelo checkIdempotency acima.
            ImportFile persisted = saveImportFilePort.save(file);
            keep = true;

            meterRegistry.counter("ecofy.ingestion.upload.total", "outcome", "accepted").increment();
            meterRegistry.summary("ecofy.ingestion.upload.bytes").record(stored.sizeBytes());

            log.info("[FileUploadService] - [upload] -> Arquivo aceito fileId={} sizeBytes={}",
                    persisted.id(), persisted.sizeBytes());

            return persisted;

        } catch (UnsupportedFileTypeException e) {
            countRejection("unsupported_content");
            throw e;
        } catch (ImportAlreadyProcessedException e) {
            meterRegistry.counter("ecofy.ingestion.duplicate.file.total").increment();
            throw e;
        } finally {
            if (!keep) {
                storeFilePort.delete(stored.storedPath());
            }
        }
    }

    // Valida os metadados declarados antes de qualquer leitura do conteúdo.
    private void validateDeclaredMetadata(UploadFileCommand command, IngestionProperties.Upload limits) {
        if (command.type() == null) {
            countRejection("unknown_type");
            throw new UnsupportedFileTypeException(
                    "Could not determine the file type", "reason=typeNotResolved");
        }

        String extension = extensionOf(command.originalFileName());
        if (!limits.isExtensionAllowed(extension)) {
            countRejection("extension");
            throw new UnsupportedFileTypeException(
                    "File extension is not allowed",
                    "extension=" + extension + ", allowed=" + limits.getAllowedExtensions());
        }

        if (!limits.isMimeTypeAllowed(command.declaredContentType())) {
            countRejection("mime");
            throw new UnsupportedFileTypeException(
                    "Content type is not allowed",
                    "declaredContentType=" + command.declaredContentType());
        }

        // Barreira antecipada: o container já corta pelo multipart, e o storage corta
        // durante a gravação. Esta só evita gravar o que já se sabe grande demais.
        long maxBytes = limits.getMaxFileSize().toBytes();
        if (command.declaredSizeBytes() > maxBytes) {
            countRejection("size_limit");
            throw new FileTooLargeException(command.declaredSizeBytes(), maxBytes);
        }
    }

    // Lê uma amostra do arquivo gravado para provar que o conteúdo condiz com o tipo declarado.
    private void validateRealContent(ImportFileType type, String storedPath) {
        byte[] sample = new byte[SNIFF_BYTES];
        int read;

        try (InputStream in = fileContentLoaderPort.open(storedPath)) {
            read = in.readNBytes(sample, 0, SNIFF_BYTES);
        } catch (IOException e) {
            throw new StorageException("Error reading stored file for content validation", e);
        }

        FileTypeValidator.validate(type, sample, read);
    }

    // Detecta replay e reuso de chave no caso comum, deixando a decisão final para a constraint do banco.
    private void checkIdempotency(UUID ownerId, String idempotencyKey, String fileHash) {
        Optional<ImportFile> sameContent = saveImportFilePort.findByOwnerAndHash(ownerId, fileHash);
        if (sameContent.isPresent()) {
            throw alreadyProcessed(sameContent.get(), fileHash);
        }

        if (idempotencyKey == null) {
            return;
        }

        Optional<ImportFile> sameKey = saveImportFilePort.findByOwnerAndIdempotencyKey(ownerId, idempotencyKey);
        if (sameKey.isPresent() && !sameKey.get().fileHash().equals(fileHash)) {
            // Mesma chave, conteúdo diferente: rejeita e NÃO associa ao job anterior.
            throw new IdempotencyKeyPayloadMismatchException(idempotencyKey);
        }
    }

    private ImportAlreadyProcessedException alreadyProcessed(ImportFile existingFile, String fileHash) {
        UUID existingJobId = loadImportJobPort.findByImportFileId(existingFile.id())
                .map(ImportJob::id)
                .orElse(null);
        return new ImportAlreadyProcessedException(existingJobId, fileHash);
    }

    // Normaliza a chave de idempotência recebida, tratando valores fora do padrão como ausentes.
    private String normalizeIdempotencyKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        int maxLength = properties.getIdempotency().getMaxKeyLength();

        if (trimmed.length() > maxLength || !trimmed.matches("^[A-Za-z0-9._:-]+$")) {
            log.warn("[FileUploadService] - [normalizeIdempotencyKey] -> Idempotency-Key inválida ignorada length={}",
                    trimmed.length());
            return null;
        }
        return trimmed;
    }

    private static String extensionOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        String base = fileName.replace("\\", "/");
        int slash = base.lastIndexOf('/');
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        int dot = base.lastIndexOf('.');
        return dot < 0 ? "" : base.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    // Sanitiza o nome original do arquivo, truncando e removendo caracteres de controle.
    private static String safeOriginalName(String original) {
        if (original == null || original.isBlank()) {
            return "upload";
        }
        String cleaned = original.replaceAll("[\\p{Cntrl}]", "");
        return cleaned.length() > 255 ? cleaned.substring(0, 255) : cleaned;
    }

    private void countRejection(String reason) {
        meterRegistry.counter("ecofy.ingestion.upload.rejected.total", "reason", reason).increment();
    }
}

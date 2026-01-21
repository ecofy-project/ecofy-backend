package br.com.ecofy.ms_ingestion.core.application.service;

import br.com.ecofy.ms_ingestion.config.StorageProperties;
import br.com.ecofy.ms_ingestion.core.application.exception.*;
import br.com.ecofy.ms_ingestion.core.domain.ImportFile;
import br.com.ecofy.ms_ingestion.core.domain.enums.ImportFileType;
import br.com.ecofy.ms_ingestion.core.port.in.UploadFileUseCase;
import br.com.ecofy.ms_ingestion.core.port.out.SaveImportFilePort;
import br.com.ecofy.ms_ingestion.core.port.out.StoreFilePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Slf4j
@Service
public class FileUploadService implements UploadFileUseCase {

    private final SaveImportFilePort saveImportFilePort;
    private final StoreFilePort storeFilePort;
    private final StorageProperties storageProperties;

    public FileUploadService(SaveImportFilePort saveImportFilePort,
                             StoreFilePort storeFilePort,
                             StorageProperties storageProperties) {
        this.saveImportFilePort = Objects.requireNonNull(saveImportFilePort, "saveImportFilePort must not be null");
        this.storeFilePort = Objects.requireNonNull(storeFilePort, "storeFilePort must not be null");
        this.storageProperties = Objects.requireNonNull(storageProperties, "storageProperties must not be null");
    }

    // Realiza o upload validando tamanho/tipo, persistindo metadados, armazenando conteúdo e atualizando o storedPath.
    @Override
    public ImportFile upload(UploadFileCommand command) {
        if (command == null) {
            throw new IngestionException(IngestionErrorCode.INVALID_COMMAND, "command must not be null");
        }

        log.info(
                "[FileUploadService] - [upload] -> Recebendo arquivo name={} sizeBytes={}",
                command.originalFileName(), command.sizeBytes()
        );

        long sizeBytes = command.sizeBytes();
        if (sizeBytes <= 0) {
            throw new InvalidFileSizeException(sizeBytes);
        }

        long maxBytes = storageProperties.getMaxFileSizeBytes();
        if (sizeBytes > maxBytes) {
            throw new FileTooLargeException(sizeBytes, maxBytes);
        }

        ImportFileType type = command.type();
        if (type == null) {
            throw new ImportFileTypeRequiredException();
        }

        ImportFile file = ImportFile.create(
                command.originalFileName(),
                "TO_BE_DEFINED",
                type,
                sizeBytes
        );

        try {
            ImportFile persisted = saveImportFilePort.save(file);

            String storedPath;
            try {
                storedPath = storeFilePort.store(persisted, command.content());
            } catch (Exception e) {
                throw new StorageException("Failed to store file content", e);
            }

            ImportFile withPath = new ImportFile(
                    persisted.id(),
                    persisted.originalFileName(),
                    storedPath,
                    persisted.type(),
                    persisted.sizeBytes(),
                    persisted.uploadedAt()
            );

            ImportFile finalFile;
            try {
                finalFile = saveImportFilePort.save(withPath);
            } catch (Exception e) {
                throw new PersistenceException("Failed to persist ImportFile with final storedPath", e);
            }

            log.info(
                    "[FileUploadService] - [upload] -> Arquivo salvo com sucesso id={} path={}",
                    finalFile.id(), finalFile.storedPath()
            );

            return finalFile;

        } catch (IngestionException e) {
            throw e;
        } catch (Exception e) {
            throw new PersistenceException("Unexpected error while uploading file", e);
        }
    }

}

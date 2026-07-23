package br.com.ecofy.ms_ingestion.adapters.out.storage;

import br.com.ecofy.ms_ingestion.config.StorageProperties;
import br.com.ecofy.ms_ingestion.core.application.exception.StorageException;
import br.com.ecofy.ms_ingestion.core.port.out.FileContentLoaderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Objects;

// Centraliza a leitura segura dos arquivos armazenados localmente.
@Slf4j
@Component
public class LocalFileContentLoaderAdapter implements FileContentLoaderPort {

    private final StorageProperties properties;

    public LocalFileContentLoaderAdapter(StorageProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    // Abre o arquivo em streaming após validar seu confinamento no diretório base.
    @Override
    public InputStream open(String storedPath) {
        Objects.requireNonNull(storedPath, "storedPath must not be null");

        Path baseDir = Path.of(properties.getBasePath()).toAbsolutePath().normalize();
        Path target = Path.of(storedPath).toAbsolutePath().normalize();

        if (!target.startsWith(baseDir)) {
            log.error("[LocalFileContentLoaderAdapter] - [open] -> path fora do storage base");
            throw new StorageException("Stored path escapes base directory", null);
        }

        try {
            log.debug("[LocalFileContentLoaderAdapter] - [open] -> Abrindo arquivo fileName={}",
                    target.getFileName());
            return Files.newInputStream(target);
        } catch (NoSuchFileException e) {
            throw new StorageException("Stored file no longer exists", e);
        } catch (IOException e) {
            log.error("[LocalFileContentLoaderAdapter] - [open] -> Erro ao abrir arquivo error={}",
                    e.getMessage(), e);
            throw new StorageException("Error opening stored file", e);
        }
    }
}

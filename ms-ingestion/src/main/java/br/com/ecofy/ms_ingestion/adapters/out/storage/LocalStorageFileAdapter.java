package br.com.ecofy.ms_ingestion.adapters.out.storage;

import br.com.ecofy.ms_ingestion.config.StorageProperties;
import br.com.ecofy.ms_ingestion.core.domain.ImportFile;
import br.com.ecofy.ms_ingestion.core.port.out.StoreFilePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class LocalStorageFileAdapter implements StoreFilePort {

    private final StorageProperties properties;

    public LocalStorageFileAdapter(StorageProperties properties) {
        this.properties = properties;
    }

    // Persiste o arquivo no filesystem local e retorna o caminho absoluto onde foi armazenado.
    @Override
    public String store(ImportFile file, byte[] content) {
        try {
            Path baseDir = Path.of(properties.getBasePath());
            Files.createDirectories(baseDir);

            String dateDir = file.uploadedAt()
                    .atZone(java.time.ZoneOffset.UTC)
                    .toLocalDate()
                    .format(DateTimeFormatter.BASIC_ISO_DATE);

            Path dir = baseDir.resolve(dateDir);
            Files.createDirectories(dir);

            String safeName = file.id() + "-" + file.originalFileName().replaceAll("\\s+", "_");
            Path target = dir.resolve(safeName);

            Files.write(target, content, StandardOpenOption.CREATE_NEW);

            String storedPath = target.toAbsolutePath().toString();

            log.info("[LocalStorageFileAdapter] - [store] -> Arquivo armazenado path={}", storedPath);
            return storedPath;
        } catch (IOException e) {
            log.error("[LocalStorageFileAdapter] - [store] -> Erro ao gravar arquivo error={}", e.getMessage(), e);
            throw new IllegalStateException("Error storing file", e);
        }

    }

}

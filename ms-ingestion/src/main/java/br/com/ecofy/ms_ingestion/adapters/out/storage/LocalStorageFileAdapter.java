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

            // Nome técnico: UUID do arquivo + nome sanitizado (impede separadores de path e caracteres perigosos).
            String safeName = file.id() + "-" + sanitizeFileName(file.originalFileName());
            Path target = dir.resolve(safeName).normalize();

            // Defesa em profundidade contra path traversal: o alvo deve permanecer sob o diretório do dia.
            if (!target.startsWith(dir)) {
                throw new IllegalStateException("Resolved storage path escapes base directory");
            }

            Files.write(target, content, StandardOpenOption.CREATE_NEW);

            String storedPath = target.toAbsolutePath().toString();

            // Não logar o caminho local completo (evita vazar layout do filesystem).
            log.info("[LocalStorageFileAdapter] - [store] -> Arquivo armazenado fileId={} storedName={}", file.id(), safeName);
            return storedPath;
        } catch (IOException e) {
            log.error("[LocalStorageFileAdapter] - [store] -> Erro ao gravar arquivo error={}", e.getMessage(), e);
            throw new IllegalStateException("Error storing file", e);
        }

    }

    /**
     * Sanitiza o nome original para uso no filesystem: remove diretórios, mantém apenas
     * o basename e substitui caracteres não seguros. O nome original completo permanece
     * como metadado (import_file.original_filename).
     */
    static String sanitizeFileName(String original) {
        if (original == null || original.isBlank()) {
            return "upload";
        }
        // Remove qualquer componente de diretório (Windows ou Unix).
        String base = original.replace("\\", "/");
        int slash = base.lastIndexOf('/');
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        // Mantém apenas caracteres seguros.
        base = base.replaceAll("[^A-Za-z0-9._-]", "_");
        // Evita nomes só com pontos/vazios (ex.: "..", ".").
        base = base.replaceAll("^\\.+", "");
        if (base.isBlank()) {
            return "upload";
        }
        // Limita tamanho para evitar nomes gigantes.
        return base.length() > 120 ? base.substring(0, 120) : base;
    }

}

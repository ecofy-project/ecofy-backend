package br.com.ecofy.ms_ingestion.adapters.out.storage;

import br.com.ecofy.ms_ingestion.core.port.out.FileContentLoaderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Component
public class LocalFileContentLoaderAdapter implements FileContentLoaderPort {

    // Carrega o conteúdo de um arquivo local em bytes a partir do path informado.
    @Override
    public byte[] load(String path) {
        try {
            log.debug("[LocalFileContentLoaderAdapter] - [load] -> Lendo arquivo path={}", path);
            return Files.readAllBytes(Path.of(path));
        } catch (IOException e) {
            log.error("[LocalFileContentLoaderAdapter] - [load] -> Erro ao ler arquivo path={} error={}",
                    path, e.getMessage(), e);
            throw new IllegalStateException("Error loading file: " + path, e);
        }
    }

}

package br.com.ecofy.ms_ingestion.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

// Configura o storage local dos arquivos recebidos, sem duplicar o limite de tamanho definido no upload.
@Configuration
@ConfigurationProperties("ecofy.ingestion.storage")
@Validated
@Getter
@Setter
@ToString
public class StorageProperties {

    // Diretório/basePath local onde os arquivos serão armazenados.
    // Ex: /var/lib/ecofy/ingestion-files ou ./data/ingestion
    @NotBlank
    private String basePath = "./data/ingestion";

    // Define onde o container materializa o multipart antes de o conteúdo chegar ao controller.
    @NotBlank
    private String tmpDir = "./data/ingestion/tmp";

    // Controla a remoção do arquivo após sucesso, desligada por padrão porque o retry relê o original.
    private boolean deleteOnSuccess = false;
}

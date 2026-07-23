package br.com.ecofy.ms_ingestion.config;

import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Set;

// Centraliza e valida as configurações externas do serviço.
@Configuration
@ConfigurationProperties("ecofy.ingestion")
@Validated
@Getter
@Setter
@ToString
public class IngestionProperties {

    @Min(1)
    private int maxErrorsPerJob = 500;

    @Min(1)
    private int batchSize = 500;

    @Min(1)
    private int maxJobsPerRetry = 10;

    @Valid
    @NotNull
    private Upload upload = new Upload();

    @Valid
    @NotNull
    private Idempotency idempotency = new Idempotency();

    @Valid
    @NotNull
    private Pagination pagination = new Pagination();

    @Valid
    @NotNull
    private Kafka kafka = new Kafka();

    // Valida as invariantes relacionadas entre diferentes configurações.
    @PostConstruct
    void validateInvariants() {
        if (upload.getMaxFieldLength() > upload.getMaxLineLength()) {
            throw new IllegalStateException(
                    "ecofy.ingestion.upload.max-field-length (" + upload.getMaxFieldLength()
                            + ") must not exceed max-line-length (" + upload.getMaxLineLength() + ")");
        }
        if (upload.getMaxRecordedErrors() > maxErrorsPerJob) {
            throw new IllegalStateException(
                    "ecofy.ingestion.upload.max-recorded-errors (" + upload.getMaxRecordedErrors()
                            + ") must not exceed ecofy.ingestion.max-errors-per-job (" + maxErrorsPerJob + ")");
        }
        if (pagination.getDefaultSize() > pagination.getMaxSize()) {
            throw new IllegalStateException(
                    "ecofy.ingestion.pagination.default-size (" + pagination.getDefaultSize()
                            + ") must not exceed max-size (" + pagination.getMaxSize() + ")");
        }
        if (!Idempotency.ALLOWED_ALGORITHMS.contains(idempotency.getAlgorithm())) {
            throw new IllegalStateException(
                    "ecofy.ingestion.idempotency.algorithm '" + idempotency.getAlgorithm()
                            + "' is not allowed; permitted: " + Idempotency.ALLOWED_ALGORITHMS);
        }
        upload.resolveCharset();
    }

    // Centraliza os limites e formatos aceitos no upload.
    @Getter
    @Setter
    @ToString
    public static class Upload {

        @NotNull
        private DataSize maxFileSize = DataSize.ofMegabytes(10);

        @Min(1)
        private long maxLines = 100_000;

        @Min(1)
        private int maxLineLength = 10_000;

        @Min(1)
        private int maxFieldLength = 2_048;

        @Min(1)
        private int maxColumns = 50;

        // Limita apenas os erros detalhados armazenados.
        @Min(1)
        private int maxRecordedErrors = 1_000;

        @NotNull
        private Duration processingTimeout = Duration.ofMinutes(10);

        @NotEmpty
        private List<String> allowedExtensions = List.of("csv", "ofx");

        @NotEmpty
        private List<String> allowedMimeTypes = List.of(
                "text/csv",
                "application/csv",
                "text/plain",
                "application/octet-stream",
                "application/x-ofx"
        );

        @NotBlank
        private String charset = StandardCharsets.UTF_8.name();

        public Charset resolveCharset() {
            return Charset.forName(charset);
        }

        public boolean isExtensionAllowed(String extension) {
            if (extension == null) {
                return false;
            }
            String normalized = extension.toLowerCase(Locale.ROOT);
            return allowedExtensions.stream().anyMatch(e -> e.toLowerCase(Locale.ROOT).equals(normalized));
        }

        // Valida o tipo MIME ignorando parâmetros opcionais.
        public boolean isMimeTypeAllowed(String mimeType) {
            if (mimeType == null || mimeType.isBlank()) {
                return true;
            }
            String base = mimeType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
            return allowedMimeTypes.stream().anyMatch(m -> m.toLowerCase(Locale.ROOT).equals(base));
        }
    }

    // Configura o cálculo e o controle das chaves de idempotência.
    @Getter
    @Setter
    @ToString
    public static class Idempotency {

        static final Set<String> ALLOWED_ALGORITHMS = Set.of("SHA-256", "SHA-512");

        @NotBlank
        private String algorithm = "SHA-256";

        @NotBlank
        private String headerName = "Idempotency-Key";

        @Min(1)
        private int maxKeyLength = 128;
    }

    // Configura os limites e os campos permitidos na paginação.
    @Getter
    @Setter
    @ToString
    public static class Pagination {

        @Min(1)
        private int defaultSize = 20;

        @Min(1)
        private int maxSize = 100;

        // Restringe a ordenação às propriedades permitidas pela API.
        @NotEmpty
        private List<String> sortableFields = List.of("createdAt", "finishedAt", "status", "fileName");
    }

    // Agrupa as configurações de retry e DLT do consumidor.
    @Getter
    @Setter
    @ToString
    public static class Kafka {

        @NotBlank
        private String dltSuffix = ".dlt";

        @NotBlank
        private String consumerGroup = "ms-ingestion-tx-consumer";

        @Valid
        @NotNull
        private KafkaRetry retry = new KafkaRetry();
    }

    // Configura a política de retentativas do consumidor.
    @Getter
    @Setter
    @ToString
    public static class KafkaRetry {

        @Min(1)
        private int maxAttempts = 3;

        @NotNull
        private Duration initialInterval = Duration.ofSeconds(1);

        @DecimalMin("1.0")
        private double multiplier = 2.0;

        @NotNull
        private Duration maxInterval = Duration.ofSeconds(10);
    }
}

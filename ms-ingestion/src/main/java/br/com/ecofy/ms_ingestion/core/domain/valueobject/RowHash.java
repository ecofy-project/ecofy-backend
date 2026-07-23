package br.com.ecofy.ms_ingestion.core.domain.valueobject;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

// Calcula a chave estável de uma linha a partir do conteúdo canonizado, não da posição no arquivo.
public final class RowHash {

    private static final String ALGORITHM = "SHA-256";

    // Usa um separador que não ocorre em campo válido, impedindo que um campo forje o limite de outro.
    private static final String FIELD_SEPARATOR = String.valueOf((char) 0x1F);

    private RowHash() {
    }

    // Canoniza os campos antes de gerar o hash, para que diferenças irrelevantes não criem chaves distintas.
    public static String of(String externalId, String description, TransactionDate date, Money amount) {
        BigDecimal normalizedAmount = amount.amount().stripTrailingZeros();

        String canonical = String.join(FIELD_SEPARATOR,
                nullSafe(externalId),
                nullSafe(description).trim().toLowerCase(Locale.ROOT),
                date.value().toString(),
                normalizedAmount.toPlainString(),
                nullSafe(amount.currency()).toUpperCase(Locale.ROOT)
        );

        return hash(canonical);
    }

    private static String hash(String canonical) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] bytes = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 é obrigatório em toda JVM; se sumiu, o ambiente está quebrado.
            throw new IllegalStateException(ALGORITHM + " not available", e);
        }
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}

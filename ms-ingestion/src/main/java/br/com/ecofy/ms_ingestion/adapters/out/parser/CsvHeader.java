package br.com.ecofy.ms_ingestion.adapters.out.parser;

import br.com.ecofy.ms_ingestion.core.application.exception.ErrorDetail;
import br.com.ecofy.ms_ingestion.core.application.exception.FileColumnLimitExceededException;
import br.com.ecofy.ms_ingestion.core.application.exception.InvalidFileHeaderException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// Valida o cabeçalho CSV e resolve as colunas por nome, aceitando ordem livre e colunas extras.
final class CsvHeader {

    // Mapeia os nomes aceitos para cada coluna canônica, preservando os formatos legado e documentado.
    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("date", "date"),
            Map.entry("transactiondate", "date"),
            Map.entry("data", "date"),
            Map.entry("description", "description"),
            Map.entry("descricao", "description"),
            Map.entry("amount", "amount"),
            Map.entry("valor", "amount"),
            Map.entry("currency", "currency"),
            Map.entry("moeda", "currency"),
            Map.entry("externalid", "externalId"),
            Map.entry("external_id", "externalId"),
            Map.entry("fitid", "externalId")
    );

    private static final List<String> REQUIRED = List.of("date", "description", "amount");

    private final Map<String, Integer> columnIndex;
    private final int columnCount;

    private CsvHeader(Map<String, Integer> columnIndex, int columnCount) {
        this.columnIndex = columnIndex;
        this.columnCount = columnCount;
    }

    int columnCount() {
        return columnCount;
    }

    // Recupera o índice da coluna canônica, ou -1 quando ela está ausente.
    int indexOf(String canonicalName) {
        return columnIndex.getOrDefault(canonicalName, -1);
    }

    // Valida e constrói o cabeçalho, ignorando colunas desconhecidas e recusando obrigatórias ausentes ou duplicadas.
    static CsvHeader parse(List<String> rawFields, int maxColumns) {
        if (rawFields.size() > maxColumns) {
            throw new FileColumnLimitExceededException(rawFields.size(), maxColumns);
        }

        Map<String, Integer> resolved = new HashMap<>();
        Set<String> seen = new HashSet<>();
        List<ErrorDetail> problems = new ArrayList<>();

        for (int i = 0; i < rawFields.size(); i++) {
            String normalized = normalize(rawFields.get(i));
            if (normalized.isEmpty()) {
                continue;
            }

            String canonical = ALIASES.get(normalized);
            if (canonical == null) {
                continue;
            }

            if (!seen.add(canonical)) {
                problems.add(ErrorDetail.ofField("header", "DUPLICATE_COLUMN",
                        "A coluna " + canonical + " aparece mais de uma vez."));
                continue;
            }
            resolved.put(canonical, i);
        }

        for (String required : REQUIRED) {
            if (!resolved.containsKey(required)) {
                problems.add(ErrorDetail.ofField("header", "MISSING_REQUIRED_COLUMN",
                        "A coluna " + required + " é obrigatória."));
            }
        }

        if (!problems.isEmpty()) {
            throw new InvalidFileHeaderException(problems);
        }

        return new CsvHeader(Map.copyOf(resolved), rawFields.size());
    }

    // Normaliza o nome da coluna para comparação, removendo espaços, BOM, aspas e diferenças de caixa.
    private static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace(String.valueOf((char) 0xFEFF), "")
                .trim()
                .replace("\"", "")
                .toLowerCase(Locale.ROOT);
    }
}

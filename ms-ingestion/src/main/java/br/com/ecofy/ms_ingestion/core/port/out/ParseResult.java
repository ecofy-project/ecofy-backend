package br.com.ecofy.ms_ingestion.core.port.out;

import br.com.ecofy.ms_ingestion.core.domain.ImportError;
import br.com.ecofy.ms_ingestion.core.domain.RawTransaction;

import java.util.List;

/**
 * Resultado de um parse (CSV/OFX): transações válidas + erros por linha.
 *
 * Permite importação parcial: uma linha inválida vira um {@link ImportError}
 * (rastreável) em vez de derrubar o job inteiro. Erros ESTRUTURAIS do arquivo
 * continuam sendo lançados como exceção pelo parser.
 */
public record ParseResult(
        List<RawTransaction> transactions,
        List<ImportError> errors
) {
    public ParseResult {
        transactions = transactions == null ? List.of() : List.copyOf(transactions);
        errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public static ParseResult of(List<RawTransaction> transactions, List<ImportError> errors) {
        return new ParseResult(transactions, errors);
    }
}

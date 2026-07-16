package br.com.ecofy.ms_ingestion.adapters.out.parser;

import br.com.ecofy.ms_ingestion.core.domain.ImportJob;
import br.com.ecofy.ms_ingestion.core.domain.RawTransaction;
import br.com.ecofy.ms_ingestion.core.port.out.ParseResult;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ApacheCommonsCsvParserAdapterTest {

    private final ApacheCommonsCsvParserAdapter parser = new ApacheCommonsCsvParserAdapter();

    private ImportJob job() {
        return ImportJob.create(UUID.randomUUID());
    }

    @Test
    void parse_validCsv_shouldProduceTransactions_noErrors() {
        String csv = """
                date;description;amount;currency
                2026-01-15;Coffee;12.50;BRL
                2026-01-16;Book;-30.00
                """;

        ParseResult result = parser.parse(job(), csv);

        assertEquals(2, result.transactions().size());
        assertTrue(result.errors().isEmpty());

        RawTransaction first = result.transactions().get(0);
        assertEquals("Coffee", first.description());
        assertEquals("BRL", first.amount().currency());
    }

    @Test
    void parse_quotedFieldWithDelimiter_shouldBeHandled() {
        String csv = """
                date;description;amount
                2026-01-15;"Coffee; extra shot";9.90
                """;

        ParseResult result = parser.parse(job(), csv);

        assertEquals(1, result.transactions().size());
        assertEquals("Coffee; extra shot", result.transactions().get(0).description());
    }

    @Test
    void parse_lineWithInvalidDate_shouldProduceTrackableError_notDropJob() {
        String csv = """
                date;description;amount
                2026-01-15;Valid;10.00
                notadate;Invalid;20.00
                """;

        ParseResult result = parser.parse(job(), csv);

        // Importação parcial: 1 válida + 1 erro rastreável (não derruba o job).
        assertEquals(1, result.transactions().size());
        assertEquals(1, result.errors().size());
        assertTrue(result.errors().get(0).errorMessage().toLowerCase().contains("date"));
        assertEquals(3, result.errors().get(0).lineNumber());
    }

    @Test
    void parse_lineWithInvalidAmount_shouldProduceError() {
        String csv = """
                date;description;amount
                2026-01-15;BadAmount;abc
                """;

        ParseResult result = parser.parse(job(), csv);

        assertTrue(result.transactions().isEmpty());
        assertEquals(1, result.errors().size());
        assertTrue(result.errors().get(0).errorMessage().toLowerCase().contains("amount"));
    }

    @Test
    void parse_lineWithTooFewFields_shouldProduceError() {
        String csv = """
                date;description;amount
                2026-01-15;OnlyTwo
                """;

        ParseResult result = parser.parse(job(), csv);

        assertTrue(result.transactions().isEmpty());
        assertEquals(1, result.errors().size());
    }

    @Test
    void parse_emptyContent_shouldReturnEmptyResult_noException() {
        ParseResult result = parser.parse(job(), "");

        assertTrue(result.transactions().isEmpty());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void parse_blankLinesAreIgnored() {
        String csv = "date;description;amount\n\n2026-01-15;Valid;10.00\n\n";

        ParseResult result = parser.parse(job(), csv);

        assertEquals(1, result.transactions().size());
        assertTrue(result.errors().isEmpty());
    }
}

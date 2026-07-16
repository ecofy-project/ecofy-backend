package br.com.ecofy.ms_ingestion.adapters.out.parser;

import br.com.ecofy.ms_ingestion.core.domain.ImportJob;
import br.com.ecofy.ms_ingestion.core.port.out.ParseResult;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OfxParserAdapterTest {

    private final OfxParserAdapter parser = new OfxParserAdapter();

    private ImportJob job() {
        return ImportJob.create(UUID.randomUUID());
    }

    @Test
    void parse_validOfx_shouldProduceTransactions() {
        String ofx = """
                <OFX><BANKMSGSRSV1><STMTTRNRS><STMTRS>
                <CURDEF>BRL
                <BANKTRANLIST>
                <STMTTRN><TRNAMT>-25.90<DTPOSTED>20260115120000<NAME>Coffee Shop<FITID>abc1</STMTTRN>
                <STMTTRN><TRNAMT>1500.00<DTPOSTED>20260116<NAME>Salary<FITID>abc2</STMTTRN>
                </BANKTRANLIST></STMTRS></STMTTRNRS></BANKMSGSRSV1></OFX>
                """;

        ParseResult result = parser.parse(job(), ofx);

        assertEquals(2, result.transactions().size());
        assertTrue(result.errors().isEmpty());
        assertEquals("Coffee Shop", result.transactions().get(0).description());
        assertEquals("BRL", result.transactions().get(0).amount().currency());
    }

    @Test
    void parse_blockMissingRequiredTags_shouldProduceTrackableError() {
        String ofx = """
                <OFX><CURDEF>BRL
                <STMTTRN><FITID>noamount<NAME>Broken</STMTTRN>
                </OFX>
                """;

        ParseResult result = parser.parse(job(), ofx);

        assertTrue(result.transactions().isEmpty());
        assertEquals(1, result.errors().size());
    }
}

package br.com.ecofy.ms_ingestion.adapters.out.parser;

import br.com.ecofy.ms_ingestion.core.domain.ImportJob;
import br.com.ecofy.ms_ingestion.core.domain.RawTransaction;
import br.com.ecofy.ms_ingestion.core.domain.enums.TransactionSourceType;
import br.com.ecofy.ms_ingestion.core.domain.valueobject.Money;
import br.com.ecofy.ms_ingestion.core.domain.valueobject.TransactionDate;
import br.com.ecofy.ms_ingestion.core.port.out.ParseOfxPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class OfxParserAdapter implements ParseOfxPort {

    private static final Pattern STMTTRN_BLOCK =
            Pattern.compile("(?is)<STMTTRN>(.*?)</STMTTRN>");

    // Extrai blocos STMTTRN de um OFX e converte em RawTransaction, aplicando defaults e validações mínimas.
    @Override
    public List<RawTransaction> parse(ImportJob job, String ofxContent) {
        Objects.requireNonNull(job, "job must not be null");
        Objects.requireNonNull(ofxContent, "ofxContent must not be null");

        log.info("[OfxParserAdapter] - [parse] -> Parsing OFX para jobId={}", job.id());

        String normalized = normalize(ofxContent);

        // moeda default do arquivo (se existir CURDEF no topo do OFX)
        String curDef = firstTagValue(normalized, "CURDEF"); // pode ser BRL, USD, etc.

        Matcher matcher = STMTTRN_BLOCK.matcher(normalized);

        List<RawTransaction> out = new ArrayList<>();
        int idx = 0;

        while (matcher.find()) {
            idx++;
            String block = matcher.group(1);

            String trnAmtRaw = firstTagValue(block, "TRNAMT");
            String dtPostedRaw = firstTagValue(block, "DTPOSTED");
            String fitId = firstTagValue(block, "FITID");
            String name = firstTagValue(block, "NAME");
            String memo = firstTagValue(block, "MEMO");

            if (trnAmtRaw == null || dtPostedRaw == null) {
                log.warn(
                        "[OfxParserAdapter] - [parse] -> STMTTRN inválido (sem TRNAMT/DTPOSTED) idx={} jobId={} TRNAMT={} DTPOSTED={}",
                        idx, job.id(), trnAmtRaw, dtPostedRaw
                );
                continue;
            }

            BigDecimal amountValue = parseAmount(trnAmtRaw);
            Instant postedAt = parseOfxToInstant(dtPostedRaw);

            String description = resolveDescription(name, memo);

            TransactionSourceType sourceType = TransactionSourceType.FILE_OFX;

            LocalDate postedDateUtc = postedAt.atZone(ZoneOffset.UTC).toLocalDate();
            TransactionDate date = TransactionDate.of(postedDateUtc);

            Money amount = (curDef == null)
                    ? Money.of(amountValue)
                    : Money.of(amountValue, curDef);

            RawTransaction tx = RawTransaction.create(
                    job.id(),
                    fitId,
                    description,
                    date,
                    amount,
                    sourceType
            );

            out.add(tx);
        }

        log.info("[OfxParserAdapter] - [parse] -> OFX parseado totalTx={} jobId={}", out.size(), job.id());
        return out;
    }

    // Define a melhor descrição para a transação priorizando NAME e fallback para MEMO/UNKNOWN.
    private static String resolveDescription(String name, String memo) {
        if (name != null && !name.isBlank()) return name.trim();
        if (memo != null && !memo.isBlank()) return memo.trim();
        return "UNKNOWN";
    }

    // Normaliza quebras de linha e remove BOM para facilitar regex/parsing do OFX.
    private static String normalize(String s) {
        String out = s.replace("\uFEFF", "");
        out = out.replace("\r\n", "\n").replace("\r", "\n");
        return out;
    }

    // Extrai o primeiro valor associado a uma tag OFX (SGML), suportando tags sem fechamento explícito.
    private static String firstTagValue(String content, String tag) {
        if (content == null) return null;

        // OFX SGML geralmente é "<TAG>valor" (às vezes sem </TAG>)
        Pattern p = Pattern.compile(
                "(?is)<" + Pattern.quote(tag) + ">(.*?)(?:</" + Pattern.quote(tag) + ">|\\n|<)"
        );

        Matcher m = p.matcher(content);
        if (!m.find()) return null;

        String v = m.group(1);
        if (v == null) return null;

        v = v.trim();
        return v.isEmpty() ? null : v;
    }

    // Converte o valor monetário do OFX para BigDecimal, ajustando separador decimal quando necessário.
    private static BigDecimal parseAmount(String raw) {
        String cleaned = raw.trim().replace(",", ".");
        return new BigDecimal(cleaned);
    }

    // Converte o DTPOSTED do OFX (com possíveis timezones/metadados) para Instant em UTC.
    private static Instant parseOfxToInstant(String raw) {
        // Exemplos:
        // 20250105120000
        // 20250105
        // 20250105120000[-3:BRT]
        String digitsOnly = raw.replaceAll("[^0-9]", "");

        if (digitsOnly.length() >= 14) {
            String ts = digitsOnly.substring(0, 14);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.ROOT);
            LocalDateTime ldt = LocalDateTime.parse(ts, fmt);
            return ldt.toInstant(ZoneOffset.UTC);
        }

        if (digitsOnly.length() >= 8) {
            String dt = digitsOnly.substring(0, 8);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.ROOT);
            LocalDate d = LocalDate.parse(dt, fmt);
            return d.atStartOfDay(ZoneOffset.UTC).toInstant();
        }

        throw new IllegalArgumentException("DTPOSTED inválido: " + raw);
    }

}

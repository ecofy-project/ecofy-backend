package br.com.ecofy.ms_ingestion.core.port.out;

import br.com.ecofy.ms_ingestion.core.domain.RawTransaction;

import java.util.List;
import java.util.UUID;

public interface SaveRawTransactionPort {

    List<RawTransaction> saveBatch(UUID importFileId, List<RawTransaction> transactions);
}

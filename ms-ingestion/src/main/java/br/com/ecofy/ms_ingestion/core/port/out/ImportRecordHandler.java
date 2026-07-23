package br.com.ecofy.ms_ingestion.core.port.out;

import br.com.ecofy.ms_ingestion.core.domain.ImportError;
import br.com.ecofy.ms_ingestion.core.domain.RawTransaction;

public interface ImportRecordHandler {

    void onValid(RawTransaction transaction);

    void onInvalid(ImportError error);

    boolean continueProcessing();
}

package br.com.ecofy.ms_ingestion.core.port.out;

import br.com.ecofy.ms_ingestion.core.domain.ImportJob;

import java.io.Reader;

public interface ParseCsvPort {

    void parse(ImportJob job, Reader reader, ImportRecordHandler handler);
}

package br.com.ecofy.ms_ingestion.core.domain.enums;

public enum ImportFileType {

    CSV,
    OFX,

    // Origem sintética para ingestão via evento Kafka (não há arquivo físico).
    EVENT

}
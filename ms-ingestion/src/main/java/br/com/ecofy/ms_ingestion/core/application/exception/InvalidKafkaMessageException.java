package br.com.ecofy.ms_ingestion.core.application.exception;

// Sinaliza mensagem Kafka fora do contrato, erro permanente que deve seguir direto para a DLT.
public class InvalidKafkaMessageException extends IngestionException {

    public InvalidKafkaMessageException(String reason) {
        super(IngestionErrorCode.INVALID_KAFKA_MESSAGE, "Invalid Kafka message", reason);
    }
}

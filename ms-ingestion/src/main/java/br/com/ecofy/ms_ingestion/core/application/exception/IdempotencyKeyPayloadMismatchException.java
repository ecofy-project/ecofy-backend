package br.com.ecofy.ms_ingestion.core.application.exception;

// Sinaliza reuso da chave de idempotência com conteúdo diferente, o que não pode ser tratado como replay.
public class IdempotencyKeyPayloadMismatchException extends IngestionException {

    public IdempotencyKeyPayloadMismatchException(String idempotencyKey) {
        super(
                IngestionErrorCode.IDEMPOTENCY_KEY_PAYLOAD_MISMATCH,
                "Idempotency-Key was already used with a different file",
                "idempotencyKey=" + idempotencyKey
        );
    }
}

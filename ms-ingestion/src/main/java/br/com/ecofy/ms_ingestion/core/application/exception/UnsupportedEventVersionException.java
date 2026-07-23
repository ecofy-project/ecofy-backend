package br.com.ecofy.ms_ingestion.core.application.exception;

// Sinaliza versão de evento não suportada, erro permanente que deve seguir para a DLT.
public class UnsupportedEventVersionException extends IngestionException {

    public UnsupportedEventVersionException(String received, String supported) {
        super(
                IngestionErrorCode.UNSUPPORTED_EVENT_VERSION,
                "Unsupported event version",
                "received=" + received + ", supported=" + supported
        );
    }
}

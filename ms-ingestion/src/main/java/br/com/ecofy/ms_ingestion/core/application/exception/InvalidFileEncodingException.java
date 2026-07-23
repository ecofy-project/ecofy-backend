package br.com.ecofy.ms_ingestion.core.application.exception;

// Sinaliza bytes que não decodificam no charset oficial, evitando aceitar valores corrompidos como válidos.
public class InvalidFileEncodingException extends IngestionException {

    public InvalidFileEncodingException(String charset, long lineNumber) {
        super(
                IngestionErrorCode.INVALID_FILE_ENCODING,
                "The file is not valid " + charset,
                "charset=" + charset + ", nearLine=" + lineNumber
        );
    }
}

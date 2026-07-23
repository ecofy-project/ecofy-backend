package br.com.ecofy.ms_ingestion.core.application.exception;

// Sinaliza linha acima do limite de tamanho, tratada como falha global por indicar estrutura não confiável.
public class FileLineTooLongException extends IngestionException {

    private final int maxLineLength;

    public FileLineTooLongException(long lineNumber, int maxLineLength) {
        super(
                IngestionErrorCode.FILE_LINE_TOO_LONG,
                "A line exceeds the maximum allowed length",
                "line=" + lineNumber + ", maxLineLength=" + maxLineLength
        );
        this.maxLineLength = maxLineLength;
    }

    public int getMaxLineLength() {
        return maxLineLength;
    }
}

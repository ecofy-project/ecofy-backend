package br.com.ecofy.ms_ingestion.core.application.exception;

// Sinaliza arquivo com mais linhas do que o limite configurado.
public class FileLineLimitExceededException extends IngestionException {

    private final long maxLines;

    public FileLineLimitExceededException(long maxLines) {
        super(
                IngestionErrorCode.FILE_LINE_LIMIT_EXCEEDED,
                "The file exceeds the maximum number of lines allowed",
                "maxLines=" + maxLines
        );
        this.maxLines = maxLines;
    }

    public long getMaxLines() {
        return maxLines;
    }
}

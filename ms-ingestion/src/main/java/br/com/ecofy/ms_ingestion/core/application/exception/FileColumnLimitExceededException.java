package br.com.ecofy.ms_ingestion.core.application.exception;

// Sinaliza cabeçalho com mais colunas do que o limite configurado.
public class FileColumnLimitExceededException extends IngestionException {

    private final int maxColumns;

    public FileColumnLimitExceededException(int columns, int maxColumns) {
        super(
                IngestionErrorCode.FILE_COLUMN_LIMIT_EXCEEDED,
                "The file exceeds the maximum number of columns allowed",
                "columns=" + columns + ", maxColumns=" + maxColumns
        );
        this.maxColumns = maxColumns;
    }

    public int getMaxColumns() {
        return maxColumns;
    }
}

package br.com.ecofy.ms_ingestion.adapters.out.parser;

import br.com.ecofy.ms_ingestion.core.application.exception.FileLineTooLongException;
import br.com.ecofy.ms_ingestion.core.application.exception.InvalidFileEncodingException;
import br.com.ecofy.ms_ingestion.core.application.exception.StorageException;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.MalformedInputException;
import java.nio.charset.UnmappableCharacterException;

// Lê linhas com teto de tamanho, mantendo o pico de memória proporcional à linha e removendo o BOM inicial.
final class BoundedLineReader {

    private static final char BOM = 0xFEFF;

    private final Reader reader;
    private final int maxLineLength;
    private final StringBuilder buffer = new StringBuilder(256);

    private long lineNumber = 0;
    private int pushedBack = -1;
    private boolean firstRead = true;
    private boolean eof = false;

    BoundedLineReader(Reader reader, int maxLineLength) {
        this.reader = reader;
        this.maxLineLength = maxLineLength;
    }

    long lineNumber() {
        return lineNumber;
    }

    // Lê a próxima linha, ou null no fim do arquivo, falhando em linha longa demais ou encoding inválido.
    String readLine() {
        if (eof) {
            return null;
        }

        buffer.setLength(0);

        try {
            while (true) {
                int c = nextChar();

                if (c == -1) {
                    eof = true;
                    // Última linha sem terminador ainda é uma linha; só o arquivo vazio devolve null.
                    if (buffer.isEmpty()) {
                        return null;
                    }
                    lineNumber++;
                    return buffer.toString();
                }

                if (firstRead) {
                    firstRead = false;
                    if (c == BOM) {
                        continue;
                    }
                }

                if (c == '\n') {
                    lineNumber++;
                    return buffer.toString();
                }

                if (c == '\r') {
                    // Consome o \n de um \r\n; um \r solitário também termina a linha.
                    int next = read();
                    if (next != '\n' && next != -1) {
                        pushedBack = next;
                    }
                    lineNumber++;
                    return buffer.toString();
                }

                if (buffer.length() >= maxLineLength) {
                    throw new FileLineTooLongException(lineNumber + 1, maxLineLength);
                }
                buffer.append((char) c);
            }
        } catch (MalformedInputException | UnmappableCharacterException e) {
            // Decodificação estrita: bytes inválidos são erro, nunca '?' silencioso (§7.4).
            throw new InvalidFileEncodingException("UTF-8", lineNumber + 1);
        } catch (IOException e) {
            throw new StorageException("Error reading import file", e);
        }
    }

    private int nextChar() throws IOException {
        if (pushedBack != -1) {
            int c = pushedBack;
            pushedBack = -1;
            return c;
        }
        return read();
    }

    private int read() throws IOException {
        return reader.read();
    }
}

package br.com.ecofy.ms_ingestion.core.application.service;

import br.com.ecofy.ms_ingestion.core.application.exception.UnsupportedFileTypeException;
import br.com.ecofy.ms_ingestion.core.domain.enums.ImportFileType;

import java.util.Locale;

// Valida o tipo do arquivo pelo conteúdo real de uma amostra inicial, não pela extensão ou pelo MIME declarado.
final class FileTypeValidator {

    // Lista as assinaturas de formatos binários que costumam chegar renomeados.
    private static final byte[][] BLOCKED_SIGNATURES = {
            {0x50, 0x4B, 0x03, 0x04},                                  // ZIP / XLSX / ODS
            {0x50, 0x4B, 0x05, 0x06},                                  // ZIP vazio
            {0x25, 0x50, 0x44, 0x46},                                  // %PDF
            {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0},             // OLE2 (.xls legado)
            {0x1F, (byte) 0x8B},                                       // GZIP
            {0x7F, 0x45, 0x4C, 0x46},                                  // ELF
    };

    private FileTypeValidator() {
    }

    // Valida a amostra do arquivo contra o tipo declarado, rejeitando conteúdo incompatível.
    static void validate(ImportFileType type, byte[] sample, int sampleLength) {
        if (sampleLength <= 0) {
            throw new UnsupportedFileTypeException(
                    "The file content is empty", "sampleLength=0");
        }

        for (byte[] signature : BLOCKED_SIGNATURES) {
            if (startsWith(sample, sampleLength, signature)) {
                throw new UnsupportedFileTypeException(
                        "The file content is not a supported text format",
                        "reason=binarySignature, declaredType=" + type);
            }
        }

        if (containsNulByte(sample, sampleLength)) {
            // Texto UTF-8 legítimo não tem NUL. Presença quase certamente indica binário
            // (ou UTF-16 não declarado), que o parser leria como lixo.
            throw new UnsupportedFileTypeException(
                    "The file content is not valid text",
                    "reason=nulByte, declaredType=" + type);
        }

        if (type == ImportFileType.OFX) {
            validateOfxShape(sample, sampleLength);
        }
    }

    // Exige prova positiva do marcador de OFX na amostra.
    private static void validateOfxShape(byte[] sample, int sampleLength) {
        String text = new String(sample, 0, sampleLength, java.nio.charset.StandardCharsets.ISO_8859_1)
                .toUpperCase(Locale.ROOT);

        if (!text.contains("OFXHEADER") && !text.contains("<OFX")) {
            throw new UnsupportedFileTypeException(
                    "The file content does not look like OFX",
                    "reason=missingOfxMarker");
        }
    }

    private static boolean startsWith(byte[] sample, int sampleLength, byte[] signature) {
        if (sampleLength < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (sample[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean containsNulByte(byte[] sample, int sampleLength) {
        for (int i = 0; i < sampleLength; i++) {
            if (sample[i] == 0) {
                return true;
            }
        }
        return false;
    }
}

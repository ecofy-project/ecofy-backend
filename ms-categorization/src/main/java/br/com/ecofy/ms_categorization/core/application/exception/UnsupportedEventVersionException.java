package br.com.ecofy.ms_categorization.core.application.exception;

public class UnsupportedEventVersionException extends RuntimeException {

    public UnsupportedEventVersionException(String received, String supported) {
        super("Unsupported event version " + received + "; supported versions: " + supported);
    }
}

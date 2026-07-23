package br.com.ecofy.ms_categorization.core.application.exception;

import java.util.UUID;

public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException(UUID id) {
        super("Transaction not found for id: " + id);
    }

}

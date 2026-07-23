package br.com.ecofy.ms_categorization.core.application.exception;

import java.util.UUID;

public class RuleNotFoundException extends RuntimeException {

    public RuleNotFoundException(UUID id) { super("Categorization rule not found for id: " + id); }

}

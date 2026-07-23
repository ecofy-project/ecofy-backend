package br.com.ecofy.ms_categorization.core.application.exception;

import java.util.UUID;

public class CategoryNotFoundException extends RuntimeException {

    public CategoryNotFoundException(UUID id) { super("Category not found for id: " + id); }

}

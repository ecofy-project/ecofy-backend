package br.com.ecofy.ms_categorization.adapters.in.web.advice;

import br.com.ecofy.ms_categorization.core.application.exception.BusinessValidationException;
import br.com.ecofy.ms_categorization.core.application.exception.CategoryNotFoundException;
import br.com.ecofy.ms_categorization.core.application.exception.TransactionNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestControllerAdvice
public class RestExceptionHandler {

    // Converte CategoryNotFoundException em resposta HTTP 404 padronizada.
    @ExceptionHandler(CategoryNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleCategory(CategoryNotFoundException ex) {
        return new ApiErrorResponse("CAT-404", ex.getMessage(), Instant.now());
    }

    // Converte TransactionNotFoundException em resposta HTTP 404 padronizada.
    @ExceptionHandler(TransactionNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleTx(TransactionNotFoundException ex) {
        return new ApiErrorResponse("TX-404", ex.getMessage(), Instant.now());
    }

    // Converte BusinessValidationException em resposta HTTP 422 padronizada para erros de regra de negócio.
    @ExceptionHandler(BusinessValidationException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ApiErrorResponse handleBusiness(BusinessValidationException ex) {
        return new ApiErrorResponse("BUS-422", ex.getMessage(), Instant.now());
    }

    // Trata exceções não mapeadas e retorna uma resposta HTTP 500 genérica.
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiErrorResponse handleGeneric(Exception ex) {
        return new ApiErrorResponse("GEN-500", "Unexpected error", Instant.now());
    }

}

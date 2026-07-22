package br.com.ecofy.ms_budgeting.core.domain.exception;

import java.util.UUID;

// Sinaliza acesso a budget de outro usuário, sem confirmar a existência do recurso na resposta.
public class BudgetAccessForbiddenException extends RuntimeException {

    private final UUID budgetId;

    public BudgetAccessForbiddenException(UUID budgetId) {
        // Mensagem INTERNA (§4.11): contexto para logs; o handler expõe mensagem genérica segura.
        super("User is not authorized to access budget: " + budgetId);
        this.budgetId = budgetId;
    }

    public UUID getBudgetId() {
        return budgetId;
    }
}

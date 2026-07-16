package br.com.ecofy.ms_categorization.core.application.service;

import br.com.ecofy.ms_categorization.core.application.command.ManualCategorizeCommand;
import br.com.ecofy.ms_categorization.core.application.exception.CategoryNotFoundException;
import br.com.ecofy.ms_categorization.core.application.exception.TransactionNotFoundException;
import br.com.ecofy.ms_categorization.core.application.result.CategorizationResult;
import br.com.ecofy.ms_categorization.core.domain.Category;
import br.com.ecofy.ms_categorization.core.domain.CategorizationSuggestion;
import br.com.ecofy.ms_categorization.core.domain.Transaction;
import br.com.ecofy.ms_categorization.core.domain.event.CategorizationAppliedDomainEvent;
import br.com.ecofy.ms_categorization.core.domain.event.CategorizedTransactionDomainEvent;
import br.com.ecofy.ms_categorization.core.domain.valueobject.Merchant;
import br.com.ecofy.ms_categorization.core.domain.valueobject.Money;
import br.com.ecofy.ms_categorization.core.port.out.LoadCategoriesPortOut;
import br.com.ecofy.ms_categorization.core.port.out.LoadTransactionPortOut;
import br.com.ecofy.ms_categorization.core.port.out.PublishCategorizedTransactionEventPortOut;
import br.com.ecofy.ms_categorization.core.port.out.SaveSuggestionPortOut;
import br.com.ecofy.ms_categorization.core.port.out.SaveTransactionPortOut;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManualCategorizationServiceTest {

    @Mock private LoadTransactionPortOut loadTransactionPort;
    @Mock private SaveTransactionPortOut saveTransactionPort;
    @Mock private LoadCategoriesPortOut loadCategoriesPort;
    @Mock private SaveSuggestionPortOut saveSuggestionPort;
    @Mock private PublishCategorizedTransactionEventPortOut publishPort;

    private ManualCategorizationService service() {
        return new ManualCategorizationService(loadTransactionPort, saveTransactionPort,
                loadCategoriesPort, saveSuggestionPort, publishPort);
    }

    private Transaction tx(UUID id, UUID categoryId) {
        return new Transaction(id, UUID.randomUUID(), "ext", "Coffee", Merchant.of("Coffee"),
                LocalDate.of(2026, 1, 15), new Money(new BigDecimal("10.00"), "BRL"),
                "FILE_CSV", categoryId, Instant.now(), Instant.now());
    }

    private Category category(UUID id) {
        return new Category(id, "Food", "#fff", true, Instant.now(), Instant.now());
    }

    @Test
    void manual_transactionNotFound_throws() {
        var service = service();
        UUID txId = UUID.randomUUID();
        when(loadTransactionPort.findById(txId)).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class,
                () -> service.manualCategorize(new ManualCategorizeCommand(txId, UUID.randomUUID(), null)));
    }

    @Test
    void manual_categoryNotFound_throws() {
        var service = service();
        UUID txId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        when(loadTransactionPort.findById(txId)).thenReturn(Optional.of(tx(txId, null)));
        when(loadCategoriesPort.findById(categoryId)).thenReturn(Optional.empty());

        assertThrows(CategoryNotFoundException.class,
                () -> service.manualCategorize(new ManualCategorizeCommand(txId, categoryId, null)));
    }

    @Test
    void manual_noOp_whenAlreadySameCategory() {
        var service = service();
        UUID txId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        when(loadTransactionPort.findById(txId)).thenReturn(Optional.of(tx(txId, categoryId)));

        CategorizationResult result = service.manualCategorize(new ManualCategorizeCommand(txId, categoryId, null));

        assertTrue(result.categorized());
        assertEquals(categoryId, result.categoryId());
        verify(saveTransactionPort, never()).save(any());
        verifyNoInteractions(publishPort);
    }

    @Test
    void manual_success_savesAndPublishes() {
        var service = service();
        UUID txId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        when(loadTransactionPort.findById(txId)).thenReturn(Optional.of(tx(txId, null)));
        when(loadCategoriesPort.findById(categoryId)).thenReturn(Optional.of(category(categoryId)));
        when(saveTransactionPort.save(any())).thenAnswer(i -> i.getArgument(0));
        when(saveSuggestionPort.save(any())).thenAnswer(i -> i.getArgument(0));

        CategorizationResult result = service.manualCategorize(new ManualCategorizeCommand(txId, categoryId, "user pick"));

        assertTrue(result.categorized());
        assertEquals(categoryId, result.categoryId());
        assertEquals("MANUAL", result.decision());
        verify(saveTransactionPort).save(any());
        verify(publishPort).publish(any(CategorizedTransactionDomainEvent.class));
        verify(publishPort).publish(any(CategorizationAppliedDomainEvent.class));
    }
}

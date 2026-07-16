package br.com.ecofy.ms_categorization.core.application.service;

import br.com.ecofy.ms_categorization.config.CategorizationProperties;
import br.com.ecofy.ms_categorization.core.application.command.AutoCategorizeCommand;
import br.com.ecofy.ms_categorization.core.application.result.CategorizationResult;
import br.com.ecofy.ms_categorization.core.domain.CategorizationRule;
import br.com.ecofy.ms_categorization.core.domain.CategorizationSuggestion;
import br.com.ecofy.ms_categorization.core.domain.Transaction;
import br.com.ecofy.ms_categorization.core.domain.enums.MatchOperator;
import br.com.ecofy.ms_categorization.core.domain.enums.RuleStatus;
import br.com.ecofy.ms_categorization.core.domain.enums.SuggestionStatus;
import br.com.ecofy.ms_categorization.core.domain.event.CategorizationAppliedDomainEvent;
import br.com.ecofy.ms_categorization.core.domain.event.CategorizedTransactionDomainEvent;
import br.com.ecofy.ms_categorization.core.domain.valueobject.Merchant;
import br.com.ecofy.ms_categorization.core.domain.valueobject.Money;
import br.com.ecofy.ms_categorization.core.domain.valueobject.RuleCondition;
import br.com.ecofy.ms_categorization.core.port.out.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutoCategorizationServiceTest {

    @Mock private IdempotencyPortOut idempotencyPort;
    @Mock private SaveTransactionPortOut saveTransactionPort;
    @Mock private LoadRulesPortOut loadRulesPort;
    @Mock private SaveSuggestionPortOut saveSuggestionPort;
    @Mock private PublishCategorizedTransactionEventPortOut publishPort;

    private final Clock clock = Clock.fixed(Instant.parse("2026-01-15T12:00:00Z"), ZoneOffset.UTC);

    private AutoCategorizationService service() {
        CategorizationProperties props = new CategorizationProperties();
        props.getRuleEngine().setMinScoreToCategorize(10);
        props.getRuleEngine().setMaxRulesToEvaluate(50);
        props.getRuleEngine().setCreateSuggestionWhenUnmatched(true);
        return new AutoCategorizationService(props, idempotencyPort, saveTransactionPort, loadRulesPort,
                saveSuggestionPort, publishPort, clock, new RuleEngine());
    }

    private Transaction tx(String desc) {
        return new Transaction(UUID.randomUUID(), UUID.randomUUID(), "ext", desc, Merchant.of(desc),
                LocalDate.of(2026, 1, 15), new Money(new BigDecimal("42.90"), "BRL"),
                "FILE_CSV", null, Instant.now(clock), Instant.now(clock));
    }

    private CategorizationRule matchingRule(UUID categoryId) {
        return new CategorizationRule(UUID.randomUUID(), categoryId, "coffee-rule", RuleStatus.ACTIVE, 1,
                List.of(new RuleCondition("description", MatchOperator.CONTAINS, "coffee", 1)),
                Instant.now(clock), Instant.now(clock));
    }

    @Test
    void autoCategorize_idempotentReplay_returnsExistingWithoutReprocessing() {
        var service = service();
        Transaction inbound = tx("Coffee Shop");
        when(idempotencyPort.tryAcquire(anyString(), any())).thenReturn(false);

        UUID categoryId = UUID.randomUUID();
        CategorizationSuggestion existing = new CategorizationSuggestion(UUID.randomUUID(), inbound.getId(),
                categoryId, UUID.randomUUID(), SuggestionStatus.APPLIED_AUTO, 30, "x", Instant.now(clock), Instant.now(clock));
        when(saveSuggestionPort.findByTransactionId(inbound.getId())).thenReturn(Optional.of(existing));

        CategorizationResult result = service.autoCategorize(new AutoCategorizeCommand("msg:1", inbound));

        assertEquals("IDEMPOTENT_REPLAY", result.decision());
        assertTrue(result.categorized());
        assertEquals(categoryId, result.categoryId());
        verify(saveTransactionPort, never()).save(any());
        verifyNoInteractions(publishPort);
    }

    @Test
    void autoCategorize_unmatched_savesUnmatchedSuggestion_noPublish() {
        var service = service();
        Transaction inbound = tx("Random Store");
        when(idempotencyPort.tryAcquire(anyString(), any())).thenReturn(true);
        when(saveTransactionPort.save(any())).thenAnswer(i -> i.getArgument(0));
        when(loadRulesPort.findActiveOrdered()).thenReturn(List.of());

        CategorizationResult result = service.autoCategorize(new AutoCategorizeCommand("msg:2", inbound));

        assertEquals("UNMATCHED", result.decision());
        assertFalse(result.categorized());
        verify(saveSuggestionPort).save(any());
        verifyNoInteractions(publishPort);
    }

    @Test
    void autoCategorize_matched_publishesDownstreamEventWithBudgetingFields() {
        var service = service();
        Transaction inbound = tx("Coffee Shop");
        UUID categoryId = UUID.randomUUID();

        when(idempotencyPort.tryAcquire(anyString(), any())).thenReturn(true);
        when(saveTransactionPort.save(any())).thenAnswer(i -> i.getArgument(0));
        when(loadRulesPort.findActiveOrdered()).thenReturn(List.of(matchingRule(categoryId)));
        when(saveSuggestionPort.save(any())).thenAnswer(i -> i.getArgument(0));

        CategorizationResult result = service.autoCategorize(new AutoCategorizeCommand("msg:3", inbound));

        assertTrue(result.categorized());
        assertEquals(categoryId, result.categoryId());

        // Evento downstream (para ms-budgeting) com campos suficientes.
        ArgumentCaptor<CategorizedTransactionDomainEvent> evCaptor =
                ArgumentCaptor.forClass(CategorizedTransactionDomainEvent.class);
        verify(publishPort).publish(evCaptor.capture());
        CategorizedTransactionDomainEvent ev = evCaptor.getValue();
        assertEquals(inbound.getId(), ev.transactionId());
        assertEquals(categoryId, ev.categoryId());
        assertEquals(new BigDecimal("42.90"), ev.amount());
        assertEquals("BRL", ev.currency().getCurrencyCode());
        assertEquals(LocalDate.of(2026, 1, 15), ev.transactionDate());

        verify(publishPort).publish(any(CategorizationAppliedDomainEvent.class));
    }
}

package br.com.ecofy.ms_budgeting.adapters.out.persistence;

import br.com.ecofy.ms_budgeting.adapters.out.persistence.entity.BudgetAlertEntity;
import br.com.ecofy.ms_budgeting.adapters.out.persistence.repository.BudgetAlertRepository;
import br.com.ecofy.ms_budgeting.core.domain.BudgetAlert;
import br.com.ecofy.ms_budgeting.core.domain.enums.AlertSeverity;
import br.com.ecofy.ms_budgeting.core.port.out.SaveBudgetAlertPort;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BudgetAlertJpaAdapterTest {

    private static final UUID ID =
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private static final UUID BUDGET_ID =
            UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private static final UUID CONSUMPTION_ID =
            UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    private static final LocalDate PERIOD_START =
            LocalDate.of(2026, 6, 1);

    private static final LocalDate PERIOD_END =
            LocalDate.of(2026, 6, 30);

    private static final Instant CREATED_AT =
            Instant.parse("2026-06-25T10:30:00Z");

    @Test
    void shouldCreateAdapterWithRepository() {
        BudgetAlertRepository repository = mock(BudgetAlertRepository.class);

        BudgetAlertJpaAdapter adapter = new BudgetAlertJpaAdapter(repository);

        assertNotNull(adapter);
        assertInstanceOf(SaveBudgetAlertPort.class, adapter);
    }

    @Test
    void shouldSaveBudgetAlertAndReturnRehydratedDomain() {
        BudgetAlertRepository repository = mock(BudgetAlertRepository.class);
        BudgetAlertJpaAdapter adapter = new BudgetAlertJpaAdapter(repository);

        AlertSeverity severity = anyAlertSeverity();

        BudgetAlert alert = new BudgetAlert(
                ID,
                BUDGET_ID,
                CONSUMPTION_ID,
                severity,
                " Budget reached alert threshold ",
                PERIOD_START,
                PERIOD_END,
                CREATED_AT
        );

        when(repository.save(any(BudgetAlertEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BudgetAlert result = adapter.save(alert);

        ArgumentCaptor<BudgetAlertEntity> entityCaptor =
                ArgumentCaptor.forClass(BudgetAlertEntity.class);

        verify(repository).save(entityCaptor.capture());
        verifyNoMoreInteractions(repository);

        BudgetAlertEntity savedEntity = entityCaptor.getValue();

        assertNotNull(savedEntity);
        assertEquals(ID, savedEntity.getId());
        assertEquals(BUDGET_ID, savedEntity.getBudgetId());
        assertEquals(CONSUMPTION_ID, savedEntity.getConsumptionId());
        assertEquals(severity.name(), savedEntity.getSeverity());
        assertEquals("Budget reached alert threshold", savedEntity.getMessage());
        assertEquals(PERIOD_START, savedEntity.getPeriodStart());
        assertEquals(PERIOD_END, savedEntity.getPeriodEnd());
        assertEquals(CREATED_AT, savedEntity.getCreatedAt());

        assertNotNull(result);
        assertEquals(ID, result.getId());
        assertEquals(BUDGET_ID, result.getBudgetId());
        assertEquals(CONSUMPTION_ID, result.getConsumptionId());
        assertEquals(severity, result.getSeverity());
        assertEquals("Budget reached alert threshold", result.getMessage());
        assertEquals(PERIOD_START, result.getPeriodStart());
        assertEquals(PERIOD_END, result.getPeriodEnd());
        assertEquals(CREATED_AT, result.getCreatedAt());
    }

    @Test
    void shouldReturnDomainFromEntityReturnedByRepository() {
        BudgetAlertRepository repository = mock(BudgetAlertRepository.class);
        BudgetAlertJpaAdapter adapter = new BudgetAlertJpaAdapter(repository);

        AlertSeverity inputSeverity = anyAlertSeverity();
        AlertSeverity persistedSeverity = anyAlertSeverity();

        BudgetAlert alert = new BudgetAlert(
                ID,
                BUDGET_ID,
                CONSUMPTION_ID,
                inputSeverity,
                "Input message",
                PERIOD_START,
                PERIOD_END,
                CREATED_AT
        );

        UUID persistedId =
                UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

        Instant persistedCreatedAt =
                Instant.parse("2026-06-26T10:30:00Z");

        BudgetAlertEntity persistedEntity = BudgetAlertEntity.builder()
                .id(persistedId)
                .budgetId(BUDGET_ID)
                .consumptionId(CONSUMPTION_ID)
                .severity(persistedSeverity.name())
                .message(" Persisted message ")
                .periodStart(PERIOD_START)
                .periodEnd(PERIOD_END)
                .createdAt(persistedCreatedAt)
                .build();

        when(repository.save(any(BudgetAlertEntity.class)))
                .thenReturn(persistedEntity);

        BudgetAlert result = adapter.save(alert);

        assertNotNull(result);
        assertEquals(persistedId, result.getId());
        assertEquals(BUDGET_ID, result.getBudgetId());
        assertEquals(CONSUMPTION_ID, result.getConsumptionId());
        assertEquals(persistedSeverity, result.getSeverity());
        assertEquals("Persisted message", result.getMessage());
        assertEquals(PERIOD_START, result.getPeriodStart());
        assertEquals(PERIOD_END, result.getPeriodEnd());
        assertEquals(persistedCreatedAt, result.getCreatedAt());

        verify(repository).save(any(BudgetAlertEntity.class));
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldThrowNullPointerExceptionWhenAlertIsNull() {
        BudgetAlertRepository repository = mock(BudgetAlertRepository.class);
        BudgetAlertJpaAdapter adapter = new BudgetAlertJpaAdapter(repository);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> adapter.save(null)
        );

        assertEquals("alert must not be null", exception.getMessage());

        verifyNoInteractions(repository);
    }

    @Test
    void shouldPropagateExceptionWhenMapperFailsBeforeRepositorySave() {
        BudgetAlertRepository repository = mock(BudgetAlertRepository.class);
        BudgetAlertJpaAdapter adapter = new BudgetAlertJpaAdapter(repository);

        BudgetAlert invalidAlert = new BudgetAlert(
                ID,
                BUDGET_ID,
                CONSUMPTION_ID,
                anyAlertSeverity(),
                "   ",
                PERIOD_START,
                PERIOD_END,
                CREATED_AT
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> adapter.save(invalidAlert)
        );

        assertEquals("message must not be blank", exception.getMessage());

        verifyNoInteractions(repository);
    }

    @Test
    void shouldPropagateRepositoryException() {
        BudgetAlertRepository repository = mock(BudgetAlertRepository.class);
        BudgetAlertJpaAdapter adapter = new BudgetAlertJpaAdapter(repository);

        BudgetAlert alert = validAlert();

        RuntimeException repositoryException =
                new RuntimeException("database unavailable");

        when(repository.save(any(BudgetAlertEntity.class)))
                .thenThrow(repositoryException);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> adapter.save(alert)
        );

        assertSame(repositoryException, exception);

        verify(repository).save(any(BudgetAlertEntity.class));
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldHaveComponentAnnotation() {
        assertNotNull(BudgetAlertJpaAdapter.class.getAnnotation(Component.class));
    }

    @Test
    void shouldHaveTransactionalAnnotationOnSaveMethod() throws Exception {
        Method method = BudgetAlertJpaAdapter.class.getDeclaredMethod(
                "save",
                BudgetAlert.class
        );

        assertNotNull(method.getAnnotation(Transactional.class));
    }

    private static BudgetAlert validAlert() {
        return new BudgetAlert(
                ID,
                BUDGET_ID,
                CONSUMPTION_ID,
                anyAlertSeverity(),
                "Budget reached alert threshold",
                PERIOD_START,
                PERIOD_END,
                CREATED_AT
        );
    }

    private static AlertSeverity anyAlertSeverity() {
        AlertSeverity[] values = AlertSeverity.values();

        if (values.length == 0) {
            throw new IllegalStateException("AlertSeverity enum must have at least one value");
        }

        return values[0];
    }
}
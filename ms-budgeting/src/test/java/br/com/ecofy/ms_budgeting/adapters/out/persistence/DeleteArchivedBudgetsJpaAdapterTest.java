package br.com.ecofy.ms_budgeting.adapters.out.persistence;

import br.com.ecofy.ms_budgeting.adapters.out.persistence.repository.BudgetJpaRepository;
import br.com.ecofy.ms_budgeting.core.port.out.DeleteArchivedBudgetsOlderThanPort;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeleteArchivedBudgetsJpaAdapterTest {

    private static final LocalDate CUTOFF_DATE =
            LocalDate.of(2026, 6, 30);

    @Test
    void shouldCreateAdapterWithRepository() {
        BudgetJpaRepository repository = mock(BudgetJpaRepository.class);

        DeleteArchivedBudgetsJpaAdapter adapter =
                new DeleteArchivedBudgetsJpaAdapter(repository);

        assertNotNull(adapter);
        assertInstanceOf(DeleteArchivedBudgetsOlderThanPort.class, adapter);
    }

    @Test
    void shouldDeleteArchivedBudgetsOlderThanCutoffDateAndReturnDeletedCount() {
        BudgetJpaRepository repository = mock(BudgetJpaRepository.class);

        DeleteArchivedBudgetsJpaAdapter adapter =
                new DeleteArchivedBudgetsJpaAdapter(repository);

        when(repository.deleteByStatusAndArchivedAtLessThanEqual("ARCHIVED", CUTOFF_DATE))
                .thenReturn(5L);

        long result = adapter.deleteArchivedBudgetsOlderThan(CUTOFF_DATE);

        assertEquals(5L, result);

        verify(repository).deleteByStatusAndArchivedAtLessThanEqual(
                "ARCHIVED",
                CUTOFF_DATE
        );

        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldReturnZeroWhenNoArchivedBudgetIsDeleted() {
        BudgetJpaRepository repository = mock(BudgetJpaRepository.class);

        DeleteArchivedBudgetsJpaAdapter adapter =
                new DeleteArchivedBudgetsJpaAdapter(repository);

        when(repository.deleteByStatusAndArchivedAtLessThanEqual("ARCHIVED", CUTOFF_DATE))
                .thenReturn(0L);

        long result = adapter.deleteArchivedBudgetsOlderThan(CUTOFF_DATE);

        assertEquals(0L, result);

        verify(repository).deleteByStatusAndArchivedAtLessThanEqual(
                "ARCHIVED",
                CUTOFF_DATE
        );

        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldThrowNullPointerExceptionWhenCutoffDateInclusiveIsNull() {
        BudgetJpaRepository repository = mock(BudgetJpaRepository.class);

        DeleteArchivedBudgetsJpaAdapter adapter =
                new DeleteArchivedBudgetsJpaAdapter(repository);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> adapter.deleteArchivedBudgetsOlderThan(null)
        );

        assertEquals("cutoffDateInclusive must not be null", exception.getMessage());

        verifyNoInteractions(repository);
    }

    @Test
    void shouldPropagateRepositoryException() {
        BudgetJpaRepository repository = mock(BudgetJpaRepository.class);

        DeleteArchivedBudgetsJpaAdapter adapter =
                new DeleteArchivedBudgetsJpaAdapter(repository);

        RuntimeException repositoryException =
                new RuntimeException("database unavailable");

        when(repository.deleteByStatusAndArchivedAtLessThanEqual("ARCHIVED", CUTOFF_DATE))
                .thenThrow(repositoryException);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> adapter.deleteArchivedBudgetsOlderThan(CUTOFF_DATE)
        );

        assertSame(repositoryException, exception);

        verify(repository).deleteByStatusAndArchivedAtLessThanEqual(
                "ARCHIVED",
                CUTOFF_DATE
        );

        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldHaveComponentAnnotation() {
        assertNotNull(DeleteArchivedBudgetsJpaAdapter.class.getAnnotation(Component.class));
    }

    @Test
    void shouldHaveTransactionalAnnotationOnDeleteArchivedBudgetsOlderThanMethod() throws Exception {
        Method method = DeleteArchivedBudgetsJpaAdapter.class.getDeclaredMethod(
                "deleteArchivedBudgetsOlderThan",
                LocalDate.class
        );

        Transactional transactional = method.getAnnotation(Transactional.class);

        assertNotNull(transactional);
        assertFalse(transactional.readOnly());
    }
}
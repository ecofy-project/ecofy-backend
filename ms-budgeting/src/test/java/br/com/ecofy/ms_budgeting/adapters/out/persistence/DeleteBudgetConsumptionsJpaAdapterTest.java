package br.com.ecofy.ms_budgeting.adapters.out.persistence;

import br.com.ecofy.ms_budgeting.adapters.out.persistence.repository.BudgetConsumptionJpaRepository;
import br.com.ecofy.ms_budgeting.core.port.out.DeleteBudgetConsumptionsOlderThanPort;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeleteBudgetConsumptionsJpaAdapterTest {

    private static final LocalDate CUTOFF_DATE =
            LocalDate.of(2026, 6, 30);

    @Test
    void shouldCreateAdapterWithRepository() {
        BudgetConsumptionJpaRepository repository =
                mock(BudgetConsumptionJpaRepository.class);

        DeleteBudgetConsumptionsJpaAdapter adapter =
                new DeleteBudgetConsumptionsJpaAdapter(repository);

        assertNotNull(adapter);
        assertInstanceOf(DeleteBudgetConsumptionsOlderThanPort.class, adapter);
    }

    @Test
    void shouldDeleteConsumptionsOlderThanCutoffDateAndReturnDeletedCount() {
        BudgetConsumptionJpaRepository repository =
                mock(BudgetConsumptionJpaRepository.class);

        DeleteBudgetConsumptionsJpaAdapter adapter =
                new DeleteBudgetConsumptionsJpaAdapter(repository);

        when(repository.deleteByReferenceDateLessThanEqual(CUTOFF_DATE))
                .thenReturn(7L);

        long result = adapter.deleteConsumptionsOlderThan(CUTOFF_DATE);

        assertEquals(7L, result);

        verify(repository).deleteByReferenceDateLessThanEqual(CUTOFF_DATE);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldReturnZeroWhenNoConsumptionIsDeleted() {
        BudgetConsumptionJpaRepository repository =
                mock(BudgetConsumptionJpaRepository.class);

        DeleteBudgetConsumptionsJpaAdapter adapter =
                new DeleteBudgetConsumptionsJpaAdapter(repository);

        when(repository.deleteByReferenceDateLessThanEqual(CUTOFF_DATE))
                .thenReturn(0L);

        long result = adapter.deleteConsumptionsOlderThan(CUTOFF_DATE);

        assertEquals(0L, result);

        verify(repository).deleteByReferenceDateLessThanEqual(CUTOFF_DATE);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldThrowNullPointerExceptionWhenCutoffDateInclusiveIsNull() {
        BudgetConsumptionJpaRepository repository =
                mock(BudgetConsumptionJpaRepository.class);

        DeleteBudgetConsumptionsJpaAdapter adapter =
                new DeleteBudgetConsumptionsJpaAdapter(repository);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> adapter.deleteConsumptionsOlderThan(null)
        );

        assertEquals("cutoffDateInclusive must not be null", exception.getMessage());

        verifyNoInteractions(repository);
    }

    @Test
    void shouldPropagateRepositoryException() {
        BudgetConsumptionJpaRepository repository =
                mock(BudgetConsumptionJpaRepository.class);

        DeleteBudgetConsumptionsJpaAdapter adapter =
                new DeleteBudgetConsumptionsJpaAdapter(repository);

        RuntimeException repositoryException =
                new RuntimeException("database unavailable");

        when(repository.deleteByReferenceDateLessThanEqual(CUTOFF_DATE))
                .thenThrow(repositoryException);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> adapter.deleteConsumptionsOlderThan(CUTOFF_DATE)
        );

        assertSame(repositoryException, exception);

        verify(repository).deleteByReferenceDateLessThanEqual(CUTOFF_DATE);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void shouldHaveComponentAnnotation() {
        assertNotNull(
                DeleteBudgetConsumptionsJpaAdapter.class.getAnnotation(Component.class)
        );
    }

    @Test
    void shouldHaveTransactionalAnnotationOnDeleteConsumptionsOlderThanMethod()
            throws Exception {
        Method method =
                DeleteBudgetConsumptionsJpaAdapter.class.getDeclaredMethod(
                        "deleteConsumptionsOlderThan",
                        LocalDate.class
                );

        Transactional transactional = method.getAnnotation(Transactional.class);

        assertNotNull(transactional);
        assertFalse(transactional.readOnly());
    }
}
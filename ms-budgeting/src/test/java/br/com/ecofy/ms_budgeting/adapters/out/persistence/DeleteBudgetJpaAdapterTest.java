package br.com.ecofy.ms_budgeting.adapters.out.persistence;

import br.com.ecofy.ms_budgeting.adapters.out.persistence.repository.BudgetJpaRepository;
import br.com.ecofy.ms_budgeting.core.port.out.DeleteBudgetPort;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeleteBudgetJpaAdapterTest {

    private static final UUID BUDGET_ID =
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Test
    void shouldCreateAdapterWithRepository() {
        BudgetJpaRepository repo = mock(BudgetJpaRepository.class);

        DeleteBudgetJpaAdapter adapter = new DeleteBudgetJpaAdapter(repo);

        assertNotNull(adapter);
        assertInstanceOf(DeleteBudgetPort.class, adapter);
    }

    @Test
    void shouldThrowNullPointerExceptionWhenRepositoryIsNull() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new DeleteBudgetJpaAdapter(null)
        );

        assertEquals("repo must not be null", exception.getMessage());
    }

    @Test
    void shouldDeleteByIdWhenBudgetExists() {
        BudgetJpaRepository repo = mock(BudgetJpaRepository.class);
        DeleteBudgetJpaAdapter adapter = new DeleteBudgetJpaAdapter(repo);

        when(repo.existsById(BUDGET_ID)).thenReturn(true);

        adapter.deleteById(BUDGET_ID);

        verify(repo).existsById(BUDGET_ID);
        verify(repo).deleteById(BUDGET_ID);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void shouldNotDeleteByIdWhenBudgetDoesNotExist() {
        BudgetJpaRepository repo = mock(BudgetJpaRepository.class);
        DeleteBudgetJpaAdapter adapter = new DeleteBudgetJpaAdapter(repo);

        when(repo.existsById(BUDGET_ID)).thenReturn(false);

        adapter.deleteById(BUDGET_ID);

        verify(repo).existsById(BUDGET_ID);
        verify(repo, never()).deleteById(any(UUID.class));
        verifyNoMoreInteractions(repo);
    }

    @Test
    void shouldThrowNullPointerExceptionWhenIdIsNullOnDeleteById() {
        BudgetJpaRepository repo = mock(BudgetJpaRepository.class);
        DeleteBudgetJpaAdapter adapter = new DeleteBudgetJpaAdapter(repo);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> adapter.deleteById(null)
        );

        assertEquals("id must not be null", exception.getMessage());

        verifyNoInteractions(repo);
    }

    @Test
    void shouldPropagateRepositoryExceptionWhenExistsByIdFailsOnDeleteById() {
        BudgetJpaRepository repo = mock(BudgetJpaRepository.class);
        DeleteBudgetJpaAdapter adapter = new DeleteBudgetJpaAdapter(repo);

        RuntimeException repositoryException =
                new RuntimeException("database unavailable");

        when(repo.existsById(BUDGET_ID)).thenThrow(repositoryException);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> adapter.deleteById(BUDGET_ID)
        );

        assertSame(repositoryException, exception);

        verify(repo).existsById(BUDGET_ID);
        verify(repo, never()).deleteById(any(UUID.class));
        verifyNoMoreInteractions(repo);
    }

    @Test
    void shouldPropagateRepositoryExceptionWhenDeleteByIdFails() {
        BudgetJpaRepository repo = mock(BudgetJpaRepository.class);
        DeleteBudgetJpaAdapter adapter = new DeleteBudgetJpaAdapter(repo);

        RuntimeException repositoryException =
                new RuntimeException("delete failed");

        when(repo.existsById(BUDGET_ID)).thenReturn(true);
        doThrow(repositoryException).when(repo).deleteById(BUDGET_ID);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> adapter.deleteById(BUDGET_ID)
        );

        assertSame(repositoryException, exception);

        verify(repo).existsById(BUDGET_ID);
        verify(repo).deleteById(BUDGET_ID);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void shouldReturnTrueWhenBudgetExistsById() {
        BudgetJpaRepository repo = mock(BudgetJpaRepository.class);
        DeleteBudgetJpaAdapter adapter = new DeleteBudgetJpaAdapter(repo);

        when(repo.existsById(BUDGET_ID)).thenReturn(true);

        boolean result = adapter.existsById(BUDGET_ID);

        assertTrue(result);

        verify(repo).existsById(BUDGET_ID);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void shouldReturnFalseWhenBudgetDoesNotExistById() {
        BudgetJpaRepository repo = mock(BudgetJpaRepository.class);
        DeleteBudgetJpaAdapter adapter = new DeleteBudgetJpaAdapter(repo);

        when(repo.existsById(BUDGET_ID)).thenReturn(false);

        boolean result = adapter.existsById(BUDGET_ID);

        assertFalse(result);

        verify(repo).existsById(BUDGET_ID);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void shouldThrowNullPointerExceptionWhenIdIsNullOnExistsById() {
        BudgetJpaRepository repo = mock(BudgetJpaRepository.class);
        DeleteBudgetJpaAdapter adapter = new DeleteBudgetJpaAdapter(repo);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> adapter.existsById(null)
        );

        assertEquals("id must not be null", exception.getMessage());

        verifyNoInteractions(repo);
    }

    @Test
    void shouldPropagateRepositoryExceptionWhenExistsByIdFails() {
        BudgetJpaRepository repo = mock(BudgetJpaRepository.class);
        DeleteBudgetJpaAdapter adapter = new DeleteBudgetJpaAdapter(repo);

        RuntimeException repositoryException =
                new RuntimeException("database unavailable");

        when(repo.existsById(BUDGET_ID)).thenThrow(repositoryException);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> adapter.existsById(BUDGET_ID)
        );

        assertSame(repositoryException, exception);

        verify(repo).existsById(BUDGET_ID);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void shouldHaveComponentAnnotation() {
        assertNotNull(DeleteBudgetJpaAdapter.class.getAnnotation(Component.class));
    }

    @Test
    void shouldHaveTransactionalAnnotationOnDeleteByIdMethod() throws Exception {
        Method method = DeleteBudgetJpaAdapter.class.getDeclaredMethod(
                "deleteById",
                UUID.class
        );

        Transactional transactional = method.getAnnotation(Transactional.class);

        assertNotNull(transactional);
        assertFalse(transactional.readOnly());
    }

    @Test
    void shouldNotHaveTransactionalAnnotationOnExistsByIdMethod() throws Exception {
        Method method = DeleteBudgetJpaAdapter.class.getDeclaredMethod(
                "existsById",
                UUID.class
        );

        assertNull(method.getAnnotation(Transactional.class));
    }
}
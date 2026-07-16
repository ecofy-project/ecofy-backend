package br.com.ecofy.ms_budgeting.adapters.out.persistence.repository;

import br.com.ecofy.ms_budgeting.adapters.out.persistence.entity.BudgetConsumptionEntity;
import org.junit.jupiter.api.Test;
import org.springframework.data.repository.Repository;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Correção Dia 6 (item #3): a entidade usa {@code @Id UUID}, mas o repository declarava
 * {@code JpaRepository<BudgetConsumptionEntity, Long>}. Este teste garante que o tipo do ID
 * do repository está alinhado à entidade (UUID), evitando modelagem JPA incorreta.
 */
class BudgetConsumptionJpaRepositoryIdTypeTest {

    @Test
    void repositoryIdTypeShouldBeUuidMatchingEntity() {
        ParameterizedType repo = findRepositoryInterface(BudgetConsumptionJpaRepository.class);

        Type entityType = repo.getActualTypeArguments()[0];
        Type idType = repo.getActualTypeArguments()[1];

        assertEquals(BudgetConsumptionEntity.class, entityType);
        assertEquals(UUID.class, idType, "ID do repository deve ser UUID (igual ao @Id da entidade)");
    }

    private static ParameterizedType findRepositoryInterface(Class<?> repositoryClass) {
        for (Type t : repositoryClass.getGenericInterfaces()) {
            if (t instanceof ParameterizedType pt
                    && Repository.class.isAssignableFrom((Class<?>) pt.getRawType())) {
                return pt;
            }
        }
        throw new IllegalStateException("No parameterized Spring Data repository interface found");
    }
}

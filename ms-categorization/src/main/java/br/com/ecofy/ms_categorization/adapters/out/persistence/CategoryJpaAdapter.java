package br.com.ecofy.ms_categorization.adapters.out.persistence;

import br.com.ecofy.ms_categorization.adapters.out.persistence.mapper.CategoryMapper;
import br.com.ecofy.ms_categorization.adapters.out.persistence.repository.CategoryRepository;
import br.com.ecofy.ms_categorization.core.domain.Category;
import br.com.ecofy.ms_categorization.core.port.out.LoadCategoriesPortOut;
import br.com.ecofy.ms_categorization.core.port.out.SaveCategoryPortOut;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryJpaAdapter implements LoadCategoriesPortOut, SaveCategoryPortOut {

    private final CategoryRepository repo;
    private final CategoryMapper mapper;

    // Carrega todas as categorias ativas ordenadas por nome.
    @Override
    public List<Category> findActive() {

        log.debug("[CategoryJpaAdapter] - [findActive] -> Loading active categories ordered by name");

        return repo.findByActiveTrueOrderByNameAsc()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    // Busca uma categoria por id e converte para o domínio.
    @Override
    public Optional<Category> findById(UUID id) {
        Objects.requireNonNull(id, "id must not be null");

        log.debug("[CategoryJpaAdapter] - [findById] -> categoryId={}", id);

        return repo.findById(id)
                .map(mapper::toDomain);
    }

    // Persiste uma categoria (create/update) e retorna a versão do domínio salva.
    @Override
    @Transactional
    public Category save(Category category) {
        Objects.requireNonNull(category, "category must not be null");

        log.debug(
                "[CategoryJpaAdapter] - [save] -> Saving category name={} active={}",
                category.getName(),
                category.isActive()
        );

        var savedEntity = repo.save(mapper.toEntity(category));

        log.info(
                "[CategoryJpaAdapter] - [save] -> Category persisted categoryId={} name={}",
                savedEntity.getId(),
                savedEntity.getName()
        );

        return mapper.toDomain(savedEntity);
    }

}

package br.com.ecofy.ms_categorization.core.application.service;

import br.com.ecofy.ms_categorization.core.application.command.CreateCategoryCommand;
import br.com.ecofy.ms_categorization.core.domain.Category;
import br.com.ecofy.ms_categorization.core.port.in.CreateCategoryUseCase;
import br.com.ecofy.ms_categorization.core.port.in.ListCategoriesUseCase;
import br.com.ecofy.ms_categorization.core.port.out.LoadCategoriesPortOut;
import br.com.ecofy.ms_categorization.core.port.out.SaveCategoryPortOut;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService implements CreateCategoryUseCase, ListCategoriesUseCase {

    private static final boolean DEFAULT_ACTIVE = true;

    private final SaveCategoryPortOut saveCategoryPort;
    private final LoadCategoriesPortOut loadCategoriesPort;
    private final Clock clock;

    // Construtor de conveniência para wiring padrão com Clock em UTC.
    @Autowired
    public CategoryService(SaveCategoryPortOut saveCategoryPort, LoadCategoriesPortOut loadCategoriesPort) {
        this(saveCategoryPort, loadCategoriesPort, Clock.systemUTC());
    }

    // Cria e persiste uma categoria normalizando nome/cor e aplicando defaults (active=true).
    @Override
    @Transactional
    public Category create(CreateCategoryCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        String name = normalizeName(command.name());
        if (name == null) throw new IllegalArgumentException("command.name must not be blank");

        String color = normalizeColor(command.color());

        Instant now = Instant.now(clock);

        Category saved = saveCategoryPort.save(new Category(
                UUID.randomUUID(),
                name,
                color,
                DEFAULT_ACTIVE,
                now,
                now
        ));

        log.info("[CategoryService] - [create] -> categoryId={} name={} color={} active={}",
                saved.getId(), saved.getName(), saved.getColor(), saved.isActive());

        return saved;
    }

    // Lista categorias ativas para consumo em UI e validações de domínio (ex.: criação de regras).
    @Override
    public List<Category> listActive() {
        log.debug("[CategoryService] - [listActive] -> Listing active categories");
        return loadCategoriesPort.findActive();
    }

    // Remove whitespace e converte nome vazio em null para facilitar validação.
    private static String normalizeName(String name) {
        if (name == null) return null;
        String n = name.trim();
        return n.isEmpty() ? null : n;
    }

    // Remove whitespace e converte cor vazia em null (campo opcional).
    private static String normalizeColor(String color) {
        if (color == null) return null;
        String c = color.trim();
        return c.isEmpty() ? null : c;
    }

}

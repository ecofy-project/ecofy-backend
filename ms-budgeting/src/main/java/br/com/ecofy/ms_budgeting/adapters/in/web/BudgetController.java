package br.com.ecofy.ms_budgeting.adapters.in.web;

import br.com.ecofy.ms_budgeting.adapters.in.web.dto.request.CreateBudgetRequest;
import br.com.ecofy.ms_budgeting.adapters.in.web.dto.request.UpdateBudgetRequest;
import br.com.ecofy.ms_budgeting.adapters.in.web.dto.response.BudgetOverviewResponse;
import br.com.ecofy.ms_budgeting.adapters.in.web.dto.response.BudgetResponse;
import br.com.ecofy.ms_budgeting.adapters.in.web.dto.response.PageResponse;
import br.com.ecofy.ms_budgeting.adapters.in.web.security.AuthenticatedUser;
import br.com.ecofy.ms_budgeting.config.BudgetingProperties;
import br.com.ecofy.ms_budgeting.core.application.command.CreateBudgetCommand;
import br.com.ecofy.ms_budgeting.core.application.command.DeleteBudgetCommand;
import br.com.ecofy.ms_budgeting.core.application.command.UpdateBudgetCommand;
import br.com.ecofy.ms_budgeting.core.application.exception.PaginationParameterInvalidException;
import br.com.ecofy.ms_budgeting.core.application.result.BudgetResult;
import br.com.ecofy.ms_budgeting.core.domain.exception.BudgetAccessForbiddenException;
import br.com.ecofy.ms_budgeting.core.port.in.CreateBudgetUseCase;
import br.com.ecofy.ms_budgeting.core.port.in.DeleteBudgetUseCase;
import br.com.ecofy.ms_budgeting.core.port.in.GetBudgetOverviewUseCase;
import br.com.ecofy.ms_budgeting.core.port.in.GetBudgetUseCase;
import br.com.ecofy.ms_budgeting.core.port.in.ListBudgetsUseCase;
import br.com.ecofy.ms_budgeting.core.port.in.UpdateBudgetUseCase;
import br.com.ecofy.ms_budgeting.core.port.out.PageResult;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

// Centraliza as operações HTTP de orçamentos no escopo do usuário autenticado.
@Slf4j
@RestController
@Validated
@Tag(name = "Budgeting", description = "Criação, atualização, remoção e consulta de budgets e visão geral por usuário")
@RequestMapping(path = "/api/budgeting/v1/budgets", produces = MediaType.APPLICATION_JSON_VALUE)
public class BudgetController {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final List<String> ALLOWED_SORT = List.of(
            "createdAt", "updatedAt", "periodStart", "periodEnd", "status", "categoryId");

    private final CreateBudgetUseCase createBudgetUseCase;
    private final UpdateBudgetUseCase updateBudgetUseCase;
    private final DeleteBudgetUseCase deleteBudgetUseCase;
    private final ListBudgetsUseCase listBudgetsUseCase;
    private final GetBudgetUseCase getBudgetUseCase;
    private final GetBudgetOverviewUseCase getBudgetOverviewUseCase;
    private final BudgetingProperties props;
    private final MeterRegistry meterRegistry;

    public BudgetController(CreateBudgetUseCase createBudgetUseCase,
                            UpdateBudgetUseCase updateBudgetUseCase,
                            DeleteBudgetUseCase deleteBudgetUseCase,
                            ListBudgetsUseCase listBudgetsUseCase,
                            GetBudgetUseCase getBudgetUseCase,
                            GetBudgetOverviewUseCase getBudgetOverviewUseCase,
                            BudgetingProperties props,
                            MeterRegistry meterRegistry) {
        this.createBudgetUseCase = createBudgetUseCase;
        this.updateBudgetUseCase = updateBudgetUseCase;
        this.deleteBudgetUseCase = deleteBudgetUseCase;
        this.listBudgetsUseCase = listBudgetsUseCase;
        this.getBudgetUseCase = getBudgetUseCase;
        this.getBudgetOverviewUseCase = getBudgetOverviewUseCase;
        this.props = props;
        this.meterRegistry = meterRegistry;
    }

    @Operation(summary = "Cria um budget para o usuário autenticado",
            description = "O dono é derivado do JWT; `userId` no corpo é ignorado (ECO-08). Envie `Idempotency-Key` para retries.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Budget criado",
                    content = @Content(schema = @Schema(implementation = BudgetResponse.class))),
            @ApiResponse(responseCode = "400", description = "Payload inválido / regras de domínio"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "409", description = "Conflito de idempotência / budget já existe")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BudgetResponse> create(
            @RequestHeader(name = IDEMPOTENCY_KEY_HEADER, required = false)
            @Size(min = 8, max = 200, message = "Idempotency-Key must be between 8 and 200 chars") String idempotencyKey,
            @Valid @RequestBody CreateBudgetRequest req
    ) {
        UUID owner = owner();

        log.info("[BudgetController] - [create] -> owner={} categoryId={} periodType={} currency={}",
                owner, req.categoryId(), req.periodType(), req.currency());

        var cmd = new CreateBudgetCommand(
                owner, req.categoryId(), req.periodType(), req.periodStart(), req.periodEnd(),
                req.limitAmount(), req.currency(), req.status());

        var created = createBudgetUseCase.create(cmd, idempotencyKey);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(created.id()).toUri();

        return ResponseEntity.created(location).body(BudgetResponse.from(created));
    }

    @Operation(summary = "Atualiza um budget do usuário autenticado",
            description = "Ownership verificado (ECO-08). Envie `version` para optimistic locking (ECO-11): versão desatualizada → 409.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Budget atualizado",
                    content = @Content(schema = @Schema(implementation = BudgetResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Budget de outro usuário"),
            @ApiResponse(responseCode = "404", description = "Budget não encontrado"),
            @ApiResponse(responseCode = "409", description = "Conflito concorrente (BUDGET_CONCURRENT_UPDATE) / idempotência")
    })
    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BudgetResponse> update(
            @RequestHeader(name = IDEMPOTENCY_KEY_HEADER, required = false)
            @Size(min = 8, max = 200, message = "Idempotency-Key must be between 8 and 200 chars") String idempotencyKey,
            @PathVariable @NotNull UUID id,
            @Valid @RequestBody UpdateBudgetRequest req
    ) {
        requireOwnership(id, owner());

        log.info("[BudgetController] - [update] -> id={} currency={} status={} version={}",
                id, req.currency(), req.status(), req.version());

        var cmd = new UpdateBudgetCommand(id, req.newLimitAmount(), req.currency(), req.status(), req.version());
        var updated = updateBudgetUseCase.update(cmd, idempotencyKey);

        return ResponseEntity.ok(BudgetResponse.from(updated));
    }

    @Operation(summary = "Remove um budget do usuário autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Budget removido"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Budget de outro usuário"),
            @ApiResponse(responseCode = "404", description = "Budget não encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @RequestHeader(name = IDEMPOTENCY_KEY_HEADER, required = false)
            @Size(min = 8, max = 200, message = "Idempotency-Key must be between 8 and 200 chars") String idempotencyKey,
            @PathVariable @NotNull UUID id
    ) {
        requireOwnership(id, owner());

        log.info("[BudgetController] - [delete] -> id={}", id);
        deleteBudgetUseCase.delete(new DeleteBudgetCommand(id), idempotencyKey);

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Histórico paginado de budgets do usuário autenticado",
            description = "Escopo do JWT (não aceita userId no query). `sort` aceita apenas: createdAt, updatedAt, periodStart, periodEnd, status, categoryId.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página retornada"),
            @ApiResponse(responseCode = "400", description = "Parâmetro de paginação inválido"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    @GetMapping
    public ResponseEntity<PageResponse<BudgetResponse>> list(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "sort", required = false) String sort
    ) {
        UUID owner = owner();

        int resolvedPage = resolvePage(page);
        int resolvedSize = resolveSize(size);
        SortSpec sortSpec = resolveSort(sort);

        PageResult<BudgetResult> result = listBudgetsUseCase.list(new ListBudgetsUseCase.ListBudgetsQuery(
                owner, resolvedPage, resolvedSize, sortSpec.field(), sortSpec.ascending()));

        return ResponseEntity.ok(PageResponse.from(result, BudgetResponse::from));
    }

    @Operation(summary = "Busca um budget do usuário autenticado por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Budget retornado",
                    content = @Content(schema = @Schema(implementation = BudgetResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Budget de outro usuário"),
            @ApiResponse(responseCode = "404", description = "Budget não encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<BudgetResponse> get(@PathVariable("id") @NotNull UUID id) {
        BudgetResult budget = getBudgetUseCase.get(id);
        ensureOwner(budget, owner(), id);
        return ResponseEntity.ok(BudgetResponse.from(budget));
    }

    @Operation(summary = "Visão geral dos budgets do usuário autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Overview retornado",
                    content = @Content(schema = @Schema(implementation = BudgetOverviewResponse.class))),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    @GetMapping("/overview")
    public ResponseEntity<BudgetOverviewResponse> overview() {
        UUID owner = owner();
        var view = getBudgetOverviewUseCase.overview(owner);
        return ResponseEntity.ok(BudgetOverviewResponse.from(view));
    }

    private UUID owner() {
        return AuthenticatedUser.requireOwnerId(props.security().ownerClaim());
    }

    // Valida se o orçamento solicitado pertence ao usuário autenticado.
    private void requireOwnership(UUID budgetId, UUID owner) {
        BudgetResult budget = getBudgetUseCase.get(budgetId);
        ensureOwner(budget, owner, budgetId);
    }

    private void ensureOwner(BudgetResult budget, UUID owner, UUID budgetId) {
        if (!Objects.equals(budget.userId(), owner)) {
            meterRegistry.counter("ecofy.budgeting.ownership.denied.total", "operation", "budget").increment();
            throw new BudgetAccessForbiddenException(budgetId);
        }
    }

    private int resolvePage(Integer page) {
        if (page == null) {
            return 0;
        }
        if (page < 0) {
            throw new PaginationParameterInvalidException("Field 'page' must be greater than or equal to zero");
        }
        return page;
    }

    private int resolveSize(Integer size) {
        int max = props.pagination().maxSize();
        if (size == null) {
            return props.pagination().defaultSize();
        }
        if (size < 1 || size > max) {
            throw new PaginationParameterInvalidException("Field 'size' must be between 1 and " + max);
        }
        return size;
    }

    private SortSpec resolveSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return new SortSpec("createdAt", false);
        }
        String[] parts = sort.split(",", 2);
        String field = parts[0].trim();
        if (ALLOWED_SORT.stream().noneMatch(f -> f.equals(field))) {
            throw new PaginationParameterInvalidException("Field 'sort' must be one of: " + ALLOWED_SORT);
        }
        boolean ascending = true;
        if (parts.length == 2) {
            String direction = parts[1].trim().toLowerCase(Locale.ROOT);
            if (direction.equals("desc")) {
                ascending = false;
            } else if (!direction.equals("asc")) {
                throw new PaginationParameterInvalidException("Field 'sort' direction must be 'asc' or 'desc'");
            }
        }
        return new SortSpec(field, ascending);
    }

    private record SortSpec(String field, boolean ascending) {
    }
}

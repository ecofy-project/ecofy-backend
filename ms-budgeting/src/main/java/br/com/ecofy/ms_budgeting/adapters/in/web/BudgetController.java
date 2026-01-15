package br.com.ecofy.ms_budgeting.adapters.in.web;

import br.com.ecofy.ms_budgeting.adapters.in.web.dto.response.BudgetOverviewResponse;
import br.com.ecofy.ms_budgeting.adapters.in.web.dto.response.BudgetResponse;
import br.com.ecofy.ms_budgeting.adapters.in.web.dto.request.CreateBudgetRequest;
import br.com.ecofy.ms_budgeting.adapters.in.web.dto.request.UpdateBudgetRequest;
import br.com.ecofy.ms_budgeting.core.application.command.CreateBudgetCommand;
import br.com.ecofy.ms_budgeting.core.application.command.DeleteBudgetCommand;
import br.com.ecofy.ms_budgeting.core.application.command.UpdateBudgetCommand;
import br.com.ecofy.ms_budgeting.core.port.in.CreateBudgetUseCase;
import br.com.ecofy.ms_budgeting.core.port.in.DeleteBudgetUseCase;
import br.com.ecofy.ms_budgeting.core.port.in.GetBudgetOverviewUseCase;
import br.com.ecofy.ms_budgeting.core.port.in.GetBudgetUseCase;
import br.com.ecofy.ms_budgeting.core.port.in.ListBudgetsUseCase;
import br.com.ecofy.ms_budgeting.core.port.in.UpdateBudgetUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@Tag(name = "Budgeting", description = "Criação, atualização, remoção e consulta de budgets e visão geral por usuário")
@RequestMapping(path = "/api/budgeting/v1/budgets", produces = MediaType.APPLICATION_JSON_VALUE)
public class BudgetController {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final CreateBudgetUseCase createBudgetUseCase;
    private final UpdateBudgetUseCase updateBudgetUseCase;
    private final DeleteBudgetUseCase deleteBudgetUseCase;
    private final ListBudgetsUseCase listBudgetsUseCase;
    private final GetBudgetUseCase getBudgetUseCase;
    private final GetBudgetOverviewUseCase getBudgetOverviewUseCase;

    @Operation(
            summary = "Cria um budget",
            description = """
                    Cria um novo budget para um usuário e categoria, com período, limite e status.
                    
                    Idempotência:
                    - Envie o header `Idempotency-Key` para evitar duplicidade em retries do cliente.
                    
                    Resposta:
                    - 201 (Created) com `Location` apontando para o recurso recém-criado.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Budget criado com sucesso",
                    content = @Content(schema = @Schema(implementation = BudgetResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Payload inválido / regras de domínio violadas"),
            @ApiResponse(responseCode = "401", description = "Não autenticado (JWT ausente/inválido)"),
            @ApiResponse(responseCode = "409", description = "Conflito (ex.: Idempotency-Key reutilizada com payload diferente)"),
            @ApiResponse(responseCode = "500", description = "Erro interno ao criar budget")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BudgetResponse> create(
            @RequestHeader(name = IDEMPOTENCY_KEY_HEADER, required = false)
            @Size(min = 8, max = 200, message = "Idempotency-Key must be between 8 and 200 chars")
            String idempotencyKey,
            @Valid @RequestBody CreateBudgetRequest req
    ) {
        log.info(
                "[BudgetController] - [create] -> Creating budget userId={} categoryId={} periodType={} start={} end={} currency={} status={} hasIdempotencyKey={}",
                req.userId(), req.categoryId(), req.periodType(), req.periodStart(), req.periodEnd(),
                req.currency(), req.status(), idempotencyKey != null
        );

        var cmd = new CreateBudgetCommand(
                req.userId(),
                req.categoryId(),
                req.periodType(),
                req.periodStart(),
                req.periodEnd(),
                req.limitAmount(),
                req.currency(),
                req.status()
        );

        var created = createBudgetUseCase.create(cmd, idempotencyKey);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();

        return ResponseEntity.created(location).body(BudgetResponse.from(created));
    }

    @Operation(
            summary = "Atualiza um budget",
            description = """
                    Atualiza campos mutáveis do budget (ex.: limite, moeda, status).
                    
                    Idempotência:
                    - Envie o header `Idempotency-Key` para garantir operação segura em retries.
                    
                    Resposta:
                    - 200 (OK) com o budget atualizado.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Budget atualizado com sucesso",
                    content = @Content(schema = @Schema(implementation = BudgetResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos / regras de domínio violadas"),
            @ApiResponse(responseCode = "401", description = "Não autenticado (JWT ausente/inválido)"),
            @ApiResponse(responseCode = "404", description = "Budget não encontrado"),
            @ApiResponse(responseCode = "409", description = "Conflito de idempotência"),
            @ApiResponse(responseCode = "500", description = "Erro interno ao atualizar budget")
    })
    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BudgetResponse> update(
            @RequestHeader(name = IDEMPOTENCY_KEY_HEADER, required = false)
            @Size(min = 8, max = 200, message = "Idempotency-Key must be between 8 and 200 chars")
            String idempotencyKey,
            @PathVariable @NotNull UUID id,
            @Valid @RequestBody UpdateBudgetRequest req
    ) {
        log.info(
                "[BudgetController] - [update] -> Updating budget id={} currency={} status={} hasIdempotencyKey={}",
                id, req.currency(), req.status(), idempotencyKey != null
        );

        var cmd = new UpdateBudgetCommand(id, req.newLimitAmount(), req.currency(), req.status());
        var updated = updateBudgetUseCase.update(cmd, idempotencyKey);

        return ResponseEntity.ok(BudgetResponse.from(updated));
    }

    @Operation(
            summary = "Remove um budget",
            description = """
                    Remove um budget por ID.
                    
                    Idempotência:
                    - Envie o header `Idempotency-Key` para garantir operação segura em retries.
                    
                    Resposta:
                    - 204 (No Content) em caso de sucesso.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Budget removido com sucesso"),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autenticado (JWT ausente/inválido)"),
            @ApiResponse(responseCode = "404", description = "Budget não encontrado"),
            @ApiResponse(responseCode = "409", description = "Conflito de idempotência"),
            @ApiResponse(responseCode = "500", description = "Erro interno ao remover budget")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @RequestHeader(name = IDEMPOTENCY_KEY_HEADER, required = false)
            @Size(min = 8, max = 200, message = "Idempotency-Key must be between 8 and 200 chars")
            String idempotencyKey,
            @PathVariable @NotNull UUID id
    ) {
        log.info("[BudgetController] - [delete] -> Deleting budget id={} hasIdempotencyKey={}", id, idempotencyKey != null);

        deleteBudgetUseCase.delete(new DeleteBudgetCommand(id), idempotencyKey);

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Lista budgets por usuário",
            description = """
                    Retorna todos os budgets do usuário informado.
                    
                    Resposta:
                    - 200 (OK) com a lista de budgets.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista retornada com sucesso",
                    content = @Content(schema = @Schema(implementation = BudgetResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autenticado (JWT ausente/inválido)"),
            @ApiResponse(responseCode = "500", description = "Erro interno ao listar budgets")
    })
    @GetMapping
    public ResponseEntity<List<BudgetResponse>> listByUser(@RequestParam("userId") @NotNull UUID userId) {
        log.debug("[BudgetController] - [listByUser] -> userId={}", userId);

        var list = listBudgetsUseCase.listByUser(userId).stream()
                .map(BudgetResponse::from)
                .toList();

        return ResponseEntity.ok(list);
    }

    @Operation(
            summary = "Busca budget por ID",
            description = """
                    Retorna um budget específico pelo seu ID.
                    
                    Resposta:
                    - 200 (OK) com o budget.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Budget retornado com sucesso",
                    content = @Content(schema = @Schema(implementation = BudgetResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autenticado (JWT ausente/inválido)"),
            @ApiResponse(responseCode = "404", description = "Budget não encontrado"),
            @ApiResponse(responseCode = "500", description = "Erro interno ao buscar budget")
    })
    @GetMapping("/{id}")
    public ResponseEntity<BudgetResponse> get(@PathVariable("id") @NotNull UUID id) {
        log.debug("[BudgetController] - [get] -> id={}", id);

        var budget = getBudgetUseCase.get(id);
        return ResponseEntity.ok(BudgetResponse.from(budget));
    }

    @Operation(
            summary = "Visão geral (overview) de budgets por usuário",
            description = """
                    Retorna um resumo consolidado dos budgets do usuário (ex.: totais, consumos, alertas).
                    
                    Resposta:
                    - 200 (OK) com a visão geral.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Overview retornado com sucesso",
                    content = @Content(schema = @Schema(implementation = BudgetOverviewResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autenticado (JWT ausente/inválido)"),
            @ApiResponse(responseCode = "500", description = "Erro interno ao buscar overview")
    })
    @GetMapping("/overview")
    public ResponseEntity<BudgetOverviewResponse> overview(@RequestParam("userId") @NotNull UUID userId) {
        log.debug("[BudgetController] - [overview] -> userId={}", userId);

        var view = getBudgetOverviewUseCase.overview(userId);
        return ResponseEntity.ok(BudgetOverviewResponse.from(view));
    }

}

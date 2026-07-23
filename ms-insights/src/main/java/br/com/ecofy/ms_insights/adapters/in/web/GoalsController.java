package br.com.ecofy.ms_insights.adapters.in.web;

import br.com.ecofy.ms_insights.adapters.in.web.dto.request.CreateGoalRequest;
import br.com.ecofy.ms_insights.adapters.in.web.dto.response.GoalResponse;
import br.com.ecofy.ms_insights.adapters.in.web.dto.request.UpdateGoalRequest;
import br.com.ecofy.ms_insights.core.application.command.CreateGoalCommand;
import br.com.ecofy.ms_insights.core.application.command.UpdateGoalCommand;
import br.com.ecofy.ms_insights.core.application.result.GoalResult;
import br.com.ecofy.ms_insights.core.port.in.GetGoalUseCase;
import br.com.ecofy.ms_insights.core.port.in.ListGoalsUseCase;
import br.com.ecofy.ms_insights.core.port.in.UpdateGoalUseCase;
import br.com.ecofy.ms_insights.core.application.service.GoalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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

// Expõe as operações de criação, atualização e consulta de metas.
@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@Tag(name = "Insights", description = "Gestão de goals (criação, atualização, consulta e listagem por usuário)")
@RequestMapping(path = "/api/insights/v1/goals", produces = MediaType.APPLICATION_JSON_VALUE)
public class GoalsController {

    private final UpdateGoalUseCase updateGoalUseCase;
    private final ListGoalsUseCase listGoalsUseCase;
    private final GetGoalUseCase getGoalUseCase;

    private final GoalService goalService;

    @Operation(
            summary = "Cria um goal",
            description = """
                    Cria um novo goal para um usuário, com nome, valor alvo (em cents), moeda e status.
                    
                    Resposta:
                    - 201 (Created) com `Location` apontando para o recurso recém-criado.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Goal criado com sucesso",
                    content = @Content(schema = @Schema(implementation = GoalResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Payload inválido / regras de domínio violadas"),
            @ApiResponse(responseCode = "401", description = "Não autenticado (JWT ausente/inválido)"),
            @ApiResponse(responseCode = "500", description = "Erro interno ao criar goal")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GoalResponse> create(@Valid @RequestBody CreateGoalRequest request) {
        log.info(
                "[GoalsController] - [create] -> Creating goal userId={} name={} currency={} status={}",
                request.userId(), request.name(), request.currency(), request.status()
        );

        GoalResult saved = goalService.create(new CreateGoalCommand(
                request.userId(),
                request.name(),
                request.targetCents(),
                request.currency(),
                request.status()
        ));

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.id())
                .toUri();

        return ResponseEntity.created(location).body(toResponse(saved));
    }

    @Operation(
            summary = "Atualiza um goal",
            description = """
                    Atualiza campos mutáveis do goal (ex.: nome, targetCents, moeda, status).
                    
                    Resposta:
                    - 200 (OK) com o goal atualizado.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Goal atualizado com sucesso",
                    content = @Content(schema = @Schema(implementation = GoalResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos / regras de domínio violadas"),
            @ApiResponse(responseCode = "401", description = "Não autenticado (JWT ausente/inválido)"),
            @ApiResponse(responseCode = "404", description = "Goal não encontrado"),
            @ApiResponse(responseCode = "500", description = "Erro interno ao atualizar goal")
    })
    @PutMapping(path = "/{goalId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GoalResponse> update(
            @PathVariable("goalId") @NotNull UUID goalId,
            @Valid @RequestBody UpdateGoalRequest request
    ) {
        log.info(
                "[GoalsController] - [update] -> Atualizando goal goalId={} name={} currency={} status={}",
                goalId, request.name(), request.currency(), request.status()
        );

        GoalResult updated = updateGoalUseCase.update(new UpdateGoalCommand(
                goalId,
                request.name(),
                request.targetCents(),
                request.currency(),
                request.status()
        ));

        return ResponseEntity.ok(toResponse(updated));
    }

    @Operation(
            summary = "Busca goal por ID",
            description = """
                    Retorna um goal específico pelo seu ID.
                    
                    Resposta:
                    - 200 (OK) com o goal.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Goal retornado com sucesso",
                    content = @Content(schema = @Schema(implementation = GoalResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autenticado (JWT ausente/inválido)"),
            @ApiResponse(responseCode = "404", description = "Goal não encontrado"),
            @ApiResponse(responseCode = "500", description = "Erro interno ao buscar goal")
    })
    @GetMapping("/{goalId}")
    public ResponseEntity<GoalResponse> get(@PathVariable("goalId") @NotNull UUID goalId) {
        log.debug("[GoalsController] - [get] -> goalId={}", goalId);

        GoalResult goal = getGoalUseCase.get(goalId);
        return ResponseEntity.ok(toResponse(goal));
    }

    @Operation(
            summary = "Lista goals por usuário",
            description = """
                    Retorna todos os goals do usuário informado.
                    
                    Resposta:
                    - 200 (OK) com a lista de goals.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista retornada com sucesso",
                    content = @Content(schema = @Schema(implementation = GoalResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autenticado (JWT ausente/inválido)"),
            @ApiResponse(responseCode = "500", description = "Erro interno ao listar goals")
    })
    @GetMapping
    public ResponseEntity<List<GoalResponse>> listByUser(@RequestParam("userId") @NotNull UUID userId) {
        log.debug("[GoalsController] - [listByUser] -> userId={}", userId);

        List<GoalResponse> list = listGoalsUseCase.list(userId).stream()
                .map(GoalsController::toResponse)
                .toList();

        return ResponseEntity.ok(list);
    }

    private static GoalResponse toResponse(GoalResult r) {
        return new GoalResponse(
                r.id(),
                r.userId(),
                r.name(),
                r.targetCents(),
                r.currency(),
                r.status(),
                r.createdAt(),
                r.updatedAt()
        );
    }

}

package br.com.ecofy.ms_categorization.adapters.in.web;

import br.com.ecofy.ms_categorization.adapters.in.web.dto.request.CreateRuleRequest;
import br.com.ecofy.ms_categorization.adapters.in.web.dto.response.RuleResponse;
import br.com.ecofy.ms_categorization.core.application.command.CreateRuleCommand;
import br.com.ecofy.ms_categorization.core.port.in.CreateRuleUseCase;
import br.com.ecofy.ms_categorization.core.port.in.ListRulesUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(path = "/api/categorization/v1/rules", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Rules", description = "Gestão de regras de categorização")
public class RuleController {

    private final CreateRuleUseCase createRuleUseCase;
    private final ListRulesUseCase listRulesUseCase;

    @Operation(
            summary = "Cria uma nova regra",
            description = """
                    Cria uma regra vinculada a uma categoria existente.
                    A regra pode conter múltiplas condições e prioridade.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Regra criada com sucesso",
                    content = @Content(schema = @Schema(implementation = RuleResponse.class))),
            @ApiResponse(responseCode = "400", description = "Payload inválido"),
            @ApiResponse(responseCode = "404", description = "Categoria não encontrada"),
            @ApiResponse(responseCode = "500", description = "Erro interno")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RuleResponse> create(@Valid @RequestBody CreateRuleRequest request) {

        log.debug("[RuleController] - [create] -> categoryId={} name={} priority={} status={}",
                request.categoryId(), request.name(), request.priority(), request.status());

        var cmd = new CreateRuleCommand(
                request.categoryId(),
                request.name(),
                request.status(),
                request.priority(),
                request.conditions()
        );

        var saved = createRuleUseCase.create(cmd);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();

        log.info("[RuleController] - [create] -> SUCCESS ruleId={} categoryId={} priority={}",
                saved.getId(), saved.getCategoryId(), saved.getPriority());

        return ResponseEntity.created(location).body(toResponse(saved));
    }

    @Operation(
            summary = "Lista regras ativas",
            description = """
                    Retorna regras ativas ordenadas por prioridade.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso",
                    content = @Content(schema = @Schema(implementation = RuleResponse.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno")
    })
    @GetMapping
    public ResponseEntity<List<RuleResponse>> listActive() {

        log.debug("[RuleController] - [listActive] -> Listing active rules");

        var result = listRulesUseCase.listActive().stream()
                .map(RuleController::toResponse)
                .toList();

        return ResponseEntity.ok(result);
    }

    private static RuleResponse toResponse(br.com.ecofy.ms_categorization.core.domain.CategorizationRule r) {
        return new RuleResponse(
                r.getId(),
                r.getCategoryId(),
                r.getName(),
                r.getStatus(),
                r.getPriority(),
                r.getConditions(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }

}

package br.com.ecofy.ms_categorization.adapters.in.web;

import br.com.ecofy.ms_categorization.adapters.in.web.dto.request.ManualCategorizationRequest;
import br.com.ecofy.ms_categorization.adapters.in.web.dto.response.CategorizationResponse;
import br.com.ecofy.ms_categorization.core.application.command.ManualCategorizeCommand;
import br.com.ecofy.ms_categorization.core.application.result.CategorizationResult;
import br.com.ecofy.ms_categorization.core.port.in.ManualCategorizationUseCase;
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

// Centraliza o endpoint de categorização manual de transações.
@RestController
@RequestMapping(path = "/api/categorization/v1/manual", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@Tag(name = "Manual Categorization", description = "Aplicação manual de categoria em uma transação")
@Slf4j
@RequiredArgsConstructor
public class ManualCategorizationController {

    private final ManualCategorizationUseCase useCase;

    // Aplica a categoria informada e retorna o resultado da decisão.
    @Operation(
            summary = "Aplica categorização manual em uma transação",
            description = """
                    Executa override da categorização automática.
                    Registra decisão manual, gera sugestão (se aplicável)
                    e publica eventos downstream.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categorização manual aplicada com sucesso",
                    content = @Content(schema = @Schema(implementation = CategorizationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Payload inválido"),
            @ApiResponse(responseCode = "404", description = "Transação ou categoria não encontrada"),
            @ApiResponse(responseCode = "409", description = "Conflito de estado / idempotência"),
            @ApiResponse(responseCode = "500", description = "Erro interno")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CategorizationResponse> apply(
            @Valid @RequestBody ManualCategorizationRequest request
    ) {

        log.debug(
                "[ManualCategorizationController] - [apply] -> txId={} categoryId={}",
                request.transactionId(), request.categoryId()
        );

        var cmd = new ManualCategorizeCommand(
                request.transactionId(),
                request.categoryId(),
                request.rationale()
        );

        CategorizationResult result = useCase.manualCategorize(cmd);

        log.info(
                "[ManualCategorizationController] - [apply] -> Transação categorizada com sucesso txId={} categorized={} categoryId={} decision={} score={}",
                result.transactionId(),
                result.categorized(),
                result.categoryId(),
                result.decision(),
                result.score()
        );

        return ResponseEntity.ok(toResponse(result));
    }

    private static CategorizationResponse toResponse(CategorizationResult result) {
        return new CategorizationResponse(
                result.transactionId(),
                result.categorized(),
                result.categoryId(),
                result.suggestionId(),
                result.decision(),
                result.score()
        );
    }
}

package br.com.ecofy.ms_categorization.adapters.in.web;

import br.com.ecofy.ms_categorization.adapters.in.web.dto.response.SuggestionResponse;
import br.com.ecofy.ms_categorization.core.application.result.SuggestionResult;
import br.com.ecofy.ms_categorization.core.port.in.GetSuggestionUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

// Centraliza a consulta de sugestões de categorização.
@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(path = "/api/categorization/v1/suggestions", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Suggestions", description = "Consulta de sugestão de categorização por transação")
public class SuggestionController {

    private final GetSuggestionUseCase getSuggestionUseCase;

    // Consulta a sugestão mais recente associada à transação.
    @Operation(
            summary = "Busca a última sugestão por transação",
            description = """
                    Retorna a sugestão mais recente para a transação.
                    Se não houver sugestão, retorna 404.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sugestão encontrada",
                    content = @Content(schema = @Schema(implementation = SuggestionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Transação não encontrada ou sem sugestão"),
            @ApiResponse(responseCode = "500", description = "Erro interno")
    })
    @GetMapping("/{transactionId}")
    public ResponseEntity<SuggestionResponse> getByTransactionId(@PathVariable UUID transactionId) {

        log.debug("[SuggestionController] - [getByTransactionId] -> txId={}", transactionId);

        SuggestionResult result = getSuggestionUseCase.getByTransactionId(transactionId);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(new SuggestionResponse(
                result.id(),
                result.transactionId(),
                result.categoryId(),
                result.ruleId(),
                result.status(),
                result.score(),
                result.rationale()
        ));
    }
}

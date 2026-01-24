package br.com.ecofy.ms_users.adapters.in.web;

import br.com.ecofy.ms_users.adapters.in.web.dto.response.ConnectionResponse;
import br.com.ecofy.ms_users.adapters.in.web.dto.request.CreateConnectionRequest;
import br.com.ecofy.ms_users.core.application.command.CreateConnectionCommand;
import br.com.ecofy.ms_users.core.application.result.ConnectionResult;
import br.com.ecofy.ms_users.core.port.in.CreateConnectionUseCase;
import br.com.ecofy.ms_users.core.port.in.ListConnectionsUseCase;
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
@Tag(name = "Users - Connections", description = "Criação e consulta de conexões (bancos/APIs/importações) por usuário")
@RequestMapping(path = "/api/users/v1/connections", produces = MediaType.APPLICATION_JSON_VALUE)
public class ConnectionsController {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    private final CreateConnectionUseCase createConnectionUseCase;
    private final ListConnectionsUseCase listConnectionsUseCase;

    @Operation(
            summary = "Cria uma conexão para um usuário",
            description = """
                    Cria uma conexão (ex.: BANK_API, CSV_IMPORT, OPEN_FINANCE, MANUAL) associada a um usuário.
                    
                    Idempotência:
                    - Envie o header `Idempotency-Key` para evitar duplicidade em retries.
                    
                    Resposta:
                    - 201 (Created) com `Location` apontando para o recurso recém-criado.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Conexão criada com sucesso",
                    content = @Content(schema = @Schema(implementation = ConnectionResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Payload inválido / regras de domínio violadas"),
            @ApiResponse(responseCode = "401", description = "Não autenticado (JWT ausente/inválido)"),
            @ApiResponse(responseCode = "409", description = "Conflito de idempotência"),
            @ApiResponse(responseCode = "500", description = "Erro interno ao criar conexão")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ConnectionResponse> create(
            @RequestHeader(name = IDEMPOTENCY_KEY_HEADER, required = false)
            @Size(min = 8, max = 200, message = "Idempotency-Key must be between 8 and 200 chars")
            String idempotencyKey,
            @Valid @RequestBody CreateConnectionRequest req
    ) {
        log.info(
                "[ConnectionsController] - [create] -> userId={} type={} provider={} hasMetadata={} hasIdempotencyKey={}",
                req.userId(),
                req.type(),
                req.provider(),
                req.metadata() != null && !req.metadata().isEmpty(),
                idempotencyKey != null
        );

        ConnectionResult created = createConnectionUseCase.create(new CreateConnectionCommand(
                req.userId(),
                req.type(),
                req.provider(),
                req.metadata(),
                idempotencyKey
        ));

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();

        return ResponseEntity.created(location).body(toResponse(created));
    }

    @Operation(
            summary = "Lista conexões por usuário",
            description = """
                    Retorna todas as conexões do usuário informado.
                    
                    Parâmetros:
                    - userId (obrigatório)
                    - limit (opcional): padrão 50, máximo 200
                    
                    Resposta:
                    - 200 (OK) com a lista de conexões.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista retornada com sucesso",
                    content = @Content(schema = @Schema(implementation = ConnectionResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autenticado (JWT ausente/inválido)"),
            @ApiResponse(responseCode = "500", description = "Erro interno ao listar conexões")
    })
    @GetMapping
    public ResponseEntity<List<ConnectionResponse>> list(
            @RequestParam("userId") @NotNull UUID userId,
            @RequestParam(name = "limit", defaultValue = "" + DEFAULT_LIMIT) Integer limit
    ) {
        int safeLimit = clamp(limit, DEFAULT_LIMIT, MAX_LIMIT);

        log.debug("[ConnectionsController] - [list] -> userId={} limit={}", userId, safeLimit);

        var result = listConnectionsUseCase.listByUserId(userId).stream()
                .limit(safeLimit)
                .map(ConnectionsController::toResponse)
                .toList();

        return ResponseEntity.ok(result);
    }

    private static int clamp(Integer value, int defaultValue, int max) {
        if (value == null || value < 1) return defaultValue;
        return Math.min(value, max);
    }

    private static ConnectionResponse toResponse(ConnectionResult r) {
        return new ConnectionResponse(
                r.id(),
                r.userId(),
                r.type(),
                r.provider(),
                r.metadata(),
                r.createdAt()
        );
    }

}

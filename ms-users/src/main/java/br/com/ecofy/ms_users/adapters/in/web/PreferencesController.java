package br.com.ecofy.ms_users.adapters.in.web;

import br.com.ecofy.ms_users.adapters.in.web.dto.request.UpdatePreferencesRequest;
import br.com.ecofy.ms_users.adapters.in.web.dto.response.UserPreferencesResponse;
import br.com.ecofy.ms_users.core.application.command.UpdatePreferencesCommand;
import br.com.ecofy.ms_users.core.domain.enums.PreferenceKey;
import br.com.ecofy.ms_users.core.port.in.GetUserPreferencesUseCase;
import br.com.ecofy.ms_users.core.port.in.UpdateUserPreferencesUseCase;
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

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

// Expõe operações HTTP protegidas para consulta e atualização de preferências.
@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Users - Preferences",
        description = "Consulta e atualização de preferências do usuário"
)
@RequestMapping(
        path = "/api/users/v1/preferences",
        produces = MediaType.APPLICATION_JSON_VALUE
)
public class PreferencesController {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final UpdateUserPreferencesUseCase updateUseCase;
    private final GetUserPreferencesUseCase getUseCase;
    private final br.com.ecofy.ms_users.adapters.in.web.security
            .ProfileOwnershipGuard ownershipGuard;

    // Atualiza preferências do próprio usuário com proteção idempotente.
    @Operation(
            summary = "Atualiza preferências do usuário",
            description = """
                    Atualiza preferências de um usuário a partir de um mapa chave/valor.
                    
                    Idempotência:
                    - Envie o header `Idempotency-Key` para garantir operação segura em retries.
                    
                    Política de chaves:
                    - Chaves desconhecidas são ignoradas (para forward-compatibility).
                    - Se você preferir retorno 400 em chaves inválidas, altere o método `parsePreferences(...)`.
                    
                    Resposta:
                    - 200 (OK) com o snapshot atualizado de preferências.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Preferências atualizadas com sucesso",
                    content = @Content(
                            schema = @Schema(
                                    implementation = UserPreferencesResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Parâmetros inválidos / payload inválido"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Não autenticado (JWT ausente/inválido)"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflito de idempotência"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno ao atualizar preferências"
            )
    })
    @PutMapping(
            path = "/{userId}",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<UserPreferencesResponse> update(
            @RequestHeader(
                    name = IDEMPOTENCY_KEY_HEADER,
                    required = false
            )
            @Size(
                    min = 8,
                    max = 200,
                    message = "Idempotency-Key must be between 8 and 200 chars"
            )
            String idempotencyKey,
            @PathVariable("userId") @NotNull UUID userId,
            @Valid @RequestBody UpdatePreferencesRequest req
    ) {
        Objects.requireNonNull(req, "req must not be null");
        Objects.requireNonNull(
                req.preferences(),
                "req.preferences must not be null"
        );

        ownershipGuard.assertOwnsProfile(userId);

        Map<PreferenceKey, String> parsed =
                parsePreferences(req.preferences());

        log.info(
                "[PreferencesController] - [update] -> userId={} inputKeys={} acceptedKeys={} hasIdempotencyKey={}",
                userId,
                req.preferences().size(),
                parsed.size(),
                idempotencyKey != null
        );

        var result = updateUseCase.update(
                new UpdatePreferencesCommand(
                        userId,
                        parsed,
                        idempotencyKey
                )
        );

        return ResponseEntity.ok(
                new UserPreferencesResponse(
                        result.userId(),
                        result.preferences()
                )
        );
    }

    // Recupera as preferências após validar a propriedade do perfil.
    @Operation(
            summary = "Busca preferências por usuário",
            description = """
                    Retorna o mapa de preferências do usuário.
                    
                    Resposta:
                    - 200 (OK) com as preferências.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Preferências retornadas com sucesso",
                    content = @Content(
                            schema = @Schema(
                                    implementation = UserPreferencesResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Parâmetros inválidos"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Não autenticado (JWT ausente/inválido)"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuário não encontrado (se sua regra assim definir)"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno ao buscar preferências"
            )
    })
    @GetMapping("/{userId}")
    public ResponseEntity<UserPreferencesResponse> get(
            @PathVariable("userId") @NotNull UUID userId
    ) {
        log.debug(
                "[PreferencesController] - [get] -> userId={}",
                userId
        );

        ownershipGuard.assertOwnsProfile(userId);

        var result = getUseCase.getByUserId(userId);

        return ResponseEntity.ok(
                new UserPreferencesResponse(
                        result.userId(),
                        result.preferences()
                )
        );
    }

    // Converte chaves conhecidas e ignora valores incompatíveis com o contrato.
    private static Map<PreferenceKey, String> parsePreferences(
            Map<String, String> raw
    ) {
        Map<PreferenceKey, String> map =
                new EnumMap<>(PreferenceKey.class);

        for (Map.Entry<String, String> entry : raw.entrySet()) {
            String rawKey = entry.getKey();
            String value = entry.getValue();

            if (rawKey == null || rawKey.isBlank()) {
                continue;
            }

            try {
                PreferenceKey key =
                        PreferenceKey.valueOf(rawKey.trim());
                map.put(key, value);
            } catch (IllegalArgumentException ex) {
                log.debug(
                        "[PreferencesController] - [parsePreferences] -> ignoring unknown preferenceKey={}",
                        rawKey
                );
            }
        }

        return map;
    }
}

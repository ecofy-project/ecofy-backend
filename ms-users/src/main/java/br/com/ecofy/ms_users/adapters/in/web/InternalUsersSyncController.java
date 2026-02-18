package br.com.ecofy.ms_users.adapters.in.web;

import br.com.ecofy.ms_users.adapters.in.web.dto.request.UpsertUserFromAuthRequest;
import br.com.ecofy.ms_users.core.application.result.UserProfileResult;
import br.com.ecofy.ms_users.core.port.in.UpsertUserFromAuthUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Users - Internal Sync",
        description = "Endpoints internos para sincronização de perfis a partir do ms-auth (upsert por authUserId)"
)
@RequestMapping(path = "/internal/users", produces = MediaType.APPLICATION_JSON_VALUE)
public class InternalUsersSyncController {

    private static final int AUTH_USER_ID_MIN = 3;
    private static final int AUTH_USER_ID_MAX = 120;

    private final UpsertUserFromAuthUseCase upsertUserFromAuthUseCase;

    @Operation(
            summary = "Upsert de usuário a partir do Auth (internal)",
            description = """
                    Sincroniza (cria/atualiza) o perfil do usuário no ms-users usando o identificador do Auth.
                    
                    Regras:
                    - O `authUserId` do PATH é a fonte de verdade (ignora qualquer valor divergente no body).
                    - Campos opcionais podem ser enviados parcial/gradualmente; o domínio aplica merge conforme regras.
                    
                    Retorno:
                    - 200 (OK) com o estado atual do perfil após o upsert.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Perfil sincronizado com sucesso",
                    content = @Content(schema = @Schema(implementation = UserProfileResult.class))
            ),
            @ApiResponse(responseCode = "400", description = "Payload inválido / validação falhou"),
            @ApiResponse(responseCode = "401", description = "Não autenticado (JWT ausente/inválido)"),
            @ApiResponse(responseCode = "403", description = "Sem permissão para endpoint interno"),
            @ApiResponse(responseCode = "500", description = "Erro interno ao sincronizar perfil")
    })
    @PutMapping(path = "/{authUserId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserProfileResult> upsert(
            @PathVariable
            @NotBlank(message = "authUserId must not be blank")
            @Size(min = AUTH_USER_ID_MIN, max = AUTH_USER_ID_MAX, message = "authUserId must be between 3 and 120 chars")
            String authUserId,
            @Valid @RequestBody UpsertUserFromAuthRequest body
    ) {

        // Loga o mínimo necessário (sem vazar PII em excesso). Email e nome podem ser sensíveis.
        log.info(
                "[InternalUsersSyncController] - [upsert] -> authUserId={} hasEmail={} hasNameParts={} emailVerified={} status={} locale={}",
                authUserId,
                body.email() != null && !body.email().isBlank(),
                hasAnyName(body),
                Boolean.TRUE.equals(body.emailVerified()),
                body.status(),
                body.locale()
        );

        // Garante consistência: path é a fonte de verdade
        var cmd = new UpsertUserFromAuthUseCase.Command(
                authUserId,
                body.email(),
                body.firstName(),
                body.lastName(),
                body.fullName(),
                body.emailVerified(),
                body.status(),
                body.locale()
        );

        UserProfileResult result = upsertUserFromAuthUseCase.upsert(cmd);

        log.debug(
                "[InternalUsersSyncController] - [upsert] -> done authUserId={} userId={} status={}",
                authUserId,
                result != null ? result.id() : null,
                result != null ? result.status() : null
        );

        return ResponseEntity.ok(result);
    }

    private static boolean hasAnyName(UpsertUserFromAuthRequest body) {
        return (body.fullName() != null && !body.fullName().isBlank())
                || (body.firstName() != null && !body.firstName().isBlank())
                || (body.lastName() != null && !body.lastName().isBlank());
    }
}

package br.com.ecofy.ms_users.adapters.in.web;

import br.com.ecofy.ms_users.adapters.in.web.dto.request.CreateProfileRequest;
import br.com.ecofy.ms_users.adapters.in.web.dto.request.UpdateProfileRequest;
import br.com.ecofy.ms_users.adapters.in.web.dto.response.UserProfileResponse;
import br.com.ecofy.ms_users.core.application.command.CreateUserProfileCommand;
import br.com.ecofy.ms_users.core.application.command.UpdateUserProfileCommand;
import br.com.ecofy.ms_users.core.application.result.UserProfileResult;
import br.com.ecofy.ms_users.core.port.in.CreateUserProfileUseCase;
import br.com.ecofy.ms_users.core.port.in.GetUserProfileUseCase;
import br.com.ecofy.ms_users.core.port.in.UpdateUserProfileUseCase;
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
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@Tag(name = "Users - Profile", description = "Criação, atualização e consulta do perfil de usuário")
@RequestMapping(path = "/api/users/v1/profile", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserProfileController {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final CreateUserProfileUseCase createUseCase;
    private final UpdateUserProfileUseCase updateUseCase;
    private final GetUserProfileUseCase getUseCase;

    @Operation(
            summary = "Cria o perfil do usuário",
            description = """
                    Cria um perfil de usuário (eco-user profile).
                    
                    Idempotência:
                    - Envie o header `Idempotency-Key` para evitar duplicidade em retries.
                    
                    Resposta:
                    - 201 (Created) com `Location` apontando para o recurso recém-criado.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Perfil criado com sucesso",
                    content = @Content(schema = @Schema(implementation = UserProfileResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Payload inválido / regras de domínio violadas"),
            @ApiResponse(responseCode = "401", description = "Não autenticado (JWT ausente/inválido)"),
            @ApiResponse(responseCode = "409", description = "Conflito de idempotência"),
            @ApiResponse(responseCode = "500", description = "Erro interno ao criar perfil")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserProfileResponse> create(
            @RequestHeader(name = IDEMPOTENCY_KEY_HEADER, required = false)
            @Size(min = 8, max = 200, message = "Idempotency-Key must be between 8 and 200 chars")
            String idempotencyKey,
            @Valid @RequestBody CreateProfileRequest req
    ) {
        log.info(
                "[UserProfileController] - [create] -> userId={} extAuthIdPresent={} hasEmail={} hasPhone={} hasFullName={} hasIdempotencyKey={}",
                req.userId(),
                req.externalAuthId() != null && !req.externalAuthId().isBlank(),
                req.email() != null && !req.email().isBlank(),
                req.phone() != null && !req.phone().isBlank(),
                req.fullName() != null && !req.fullName().isBlank(),
                idempotencyKey != null
        );

        UserProfileResult created = createUseCase.create(new CreateUserProfileCommand(
                req.userId(),
                req.externalAuthId(),
                req.fullName(),
                req.email(),
                req.phone(),
                idempotencyKey
        ));

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{userId}")
                .buildAndExpand(created.id())
                .toUri();

        return ResponseEntity.created(location).body(toResponse(created));
    }

    @Operation(
            summary = "Atualiza o perfil do usuário",
            description = """
                    Atualiza campos mutáveis do perfil (ex.: fullName, email, phone, status).
                    
                    Idempotência:
                    - Envie o header `Idempotency-Key` para garantir operação segura em retries.
                    
                    Resposta:
                    - 200 (OK) com o perfil atualizado.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Perfil atualizado com sucesso",
                    content = @Content(schema = @Schema(implementation = UserProfileResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos / regras de domínio violadas"),
            @ApiResponse(responseCode = "401", description = "Não autenticado (JWT ausente/inválido)"),
            @ApiResponse(responseCode = "404", description = "Perfil não encontrado"),
            @ApiResponse(responseCode = "409", description = "Conflito de idempotência"),
            @ApiResponse(responseCode = "500", description = "Erro interno ao atualizar perfil")
    })
    @PutMapping(path = "/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserProfileResponse> update(
            @RequestHeader(name = IDEMPOTENCY_KEY_HEADER, required = false)
            @Size(min = 8, max = 200, message = "Idempotency-Key must be between 8 and 200 chars")
            String idempotencyKey,
            @PathVariable("userId") @NotNull UUID userId,
            @Valid @RequestBody UpdateProfileRequest req
    ) {
        log.info(
                "[UserProfileController] - [update] -> userId={} status={} hasEmail={} hasPhone={} hasFullName={} hasIdempotencyKey={}",
                userId,
                req.status(),
                req.email() != null && !req.email().isBlank(),
                req.phone() != null && !req.phone().isBlank(),
                req.fullName() != null && !req.fullName().isBlank(),
                idempotencyKey != null
        );

        UserProfileResult updated = updateUseCase.update(new UpdateUserProfileCommand(
                userId,
                req.fullName(),
                req.email(),
                req.phone(),
                req.status(),
                idempotencyKey
        ));

        return ResponseEntity.ok(toResponse(updated));
    }

    @Operation(
            summary = "Busca perfil por userId",
            description = """
                    Retorna o perfil do usuário pelo seu ID.
                    
                    Resposta:
                    - 200 (OK) com o perfil.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Perfil retornado com sucesso",
                    content = @Content(schema = @Schema(implementation = UserProfileResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autenticado (JWT ausente/inválido)"),
            @ApiResponse(responseCode = "404", description = "Perfil não encontrado"),
            @ApiResponse(responseCode = "500", description = "Erro interno ao buscar perfil")
    })
    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> get(@PathVariable("userId") @NotNull UUID userId) {
        log.debug("[UserProfileController] - [get] -> userId={}", userId);

        var result = getUseCase.getById(userId);
        return ResponseEntity.ok(toResponse(result));
    }

    private static UserProfileResponse toResponse(UserProfileResult r) {
        return new UserProfileResponse(
                r.id(),
                r.externalAuthId(),
                r.fullName(),
                r.email(),
                r.phone(),
                r.status() != null ? r.status().name() : null,
                r.createdAt(),
                r.updatedAt()
        );
    }

}

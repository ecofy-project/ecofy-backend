package br.com.ecofy.auth.adapters.in.web;

import br.com.ecofy.auth.adapters.in.web.dto.request.AdminUserCreateRequest;
import br.com.ecofy.auth.adapters.in.web.dto.response.UserResponse;
import br.com.ecofy.auth.adapters.in.web.mapper.UserMapper;
import br.com.ecofy.auth.core.port.in.RegisterUserUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping(path = "/api/admin/users", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@Tag(name = "Admin Users", description = "Gestão de usuários administradores do ms-auth")
@Slf4j
@RequiredArgsConstructor
public class AdminUserController {

    private final RegisterUserUseCase registerUserUseCase;

    @Operation(
            summary = "Cria um novo usuário administrador",
            description = """
                    Endpoint administrativo para criação de usuários com permissões elevadas.

                    - Requer autenticação com um usuário que possua a role AUTH_ADMIN (configurado em SecurityConfig).
                    - Gera um usuário com roles padrão AUTH_ADMIN e AUTH_USER, a menos que outra lista de roles seja enviada.
                    - Marca o e-mail como confirmado automaticamente (dependendo da implementação do use case).
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Usuário admin criado com sucesso",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados inválidos ou e-mail já registrado"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Sem permissão para criar usuários admin"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno ao criar usuário admin"
            )
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserResponse> createAdmin(@Valid @RequestBody AdminUserCreateRequest request) {

        log.debug("[AdminUserController] - [createAdmin] -> Criando usuário admin email={}", request.email());

        List<String> roles = (request.roles() == null || request.roles().isEmpty())
                ? List.of("AUTH_ADMIN", "AUTH_USER")
                : request.roles();

        String locale = request.locale() != null ? request.locale() : "pt-BR";

        var cmd = new RegisterUserUseCase.RegisterUserCommand(
                request.email(),
                request.password(),
                request.firstName(),
                request.lastName(),
                locale,
                true,
                roles
        );

        var user = registerUserUseCase.register(cmd);

        log.debug(
                "[AdminUserController] - [createAdmin] -> Usuário admin criado com sucesso userId={} email={}",
                user.id().value(),
                user.email().value()
        );

        UserResponse body = UserMapper.toResponse(user);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequestUri()
                .path("/{id}")
                .buildAndExpand(user.id().value())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(body);
    }
}
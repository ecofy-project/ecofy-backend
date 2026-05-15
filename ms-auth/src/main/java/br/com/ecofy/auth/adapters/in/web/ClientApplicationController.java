package br.com.ecofy.auth.adapters.in.web;

import br.com.ecofy.auth.adapters.in.web.dto.request.ClientApplicationRequest;
import br.com.ecofy.auth.adapters.in.web.dto.response.ClientApplicationResponse;
import br.com.ecofy.auth.adapters.in.web.mapper.ClientApplicationMapper;
import br.com.ecofy.auth.core.domain.ClientApplication;
import br.com.ecofy.auth.core.port.in.RegisterClientApplicationUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
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
@RequestMapping(path = "/api/admin/clients", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@Tag(name = "Client Applications", description = "Registro e gestão de client applications OAuth2/OIDC")
@Slf4j
@RequiredArgsConstructor
public class ClientApplicationController {

    private final RegisterClientApplicationUseCase registerClientApplicationUseCase;

    @Operation(
            summary = "Registra um novo client application",
            description = """
                    Registra um novo client OAuth2/OIDC no ms-auth.

                    A API resolve automaticamente:
                    - client_id seguro
                    - client_secret (quando exigido pelo tipo de client)
                    - grants padrão por tipo de client, se não forem enviados

                    Regras importantes:
                    - Clients MACHINE_TO_MACHINE devem ter CLIENT_CREDENTIALS.
                    - Clients PUBLIC/SPA não podem usar CLIENT_CREDENTIALS.
                    - Se usar AUTHORIZATION_CODE, é obrigatório informar ao menos uma redirect_uri.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Client registrado com sucesso",
                    content = @Content(schema = @Schema(implementation = ClientApplicationResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Dados inválidos ou combinação de grants/tipo de client inválida"
            ),
            @ApiResponse(responseCode = "500", description = "Erro interno ao registrar client")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClientApplicationResponse> register(
            @Valid @RequestBody ClientApplicationRequest request
    ) {

        log.debug(
                "[ClientApplicationController] - [register] -> Registrando client name={} type={} firstParty={}",
                request.name(),
                request.clientType(),
                request.firstParty()
        );

        var cmd = new RegisterClientApplicationUseCase.RegisterClientCommand(
                request.name(),
                request.clientType(),
                request.grantTypes(),
                request.redirectUris(),
                request.scopes(),
                request.firstParty() != null ? request.firstParty() : Boolean.FALSE
        );

        ClientApplication client = registerClientApplicationUseCase.register(cmd);

        log.debug(
                "[ClientApplicationController] - [register] -> Client registrado com sucesso clientId={} type={} active={}",
                client.clientId(),
                client.clientType(),
                client.isActive()
        );

        ClientApplicationResponse body = ClientApplicationMapper.toResponse(client);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequestUri()
                .path("/{clientId}")
                .buildAndExpand(client.clientId())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(body);
    }
}
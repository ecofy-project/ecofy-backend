package br.com.ecofy.auth.adapters.in.web;

import br.com.ecofy.auth.adapters.in.web.dto.request.LoginRequest;
import br.com.ecofy.auth.adapters.in.web.dto.request.RefreshTokenRequest;
import br.com.ecofy.auth.adapters.in.web.dto.request.RevokeTokenRequest;
import br.com.ecofy.auth.adapters.in.web.dto.request.ValidateTokenRequest;
import br.com.ecofy.auth.adapters.in.web.dto.response.TokenResponse;
import br.com.ecofy.auth.adapters.in.web.dto.response.ValidateTokenResponse;
import br.com.ecofy.auth.core.port.in.AuthenticateUserUseCase;
import br.com.ecofy.auth.core.port.in.RefreshTokenUseCase;
import br.com.ecofy.auth.core.port.in.RevokeTokenUseCase;
import br.com.ecofy.auth.core.port.in.ValidateTokenUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping(path = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@Tag(name = "Authentication", description = "Endpoints de autenticação e renovação de tokens JWT/OIDC")
@Slf4j
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticateUserUseCase authenticateUserUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final RevokeTokenUseCase revokeTokenUseCase;
    private final ValidateTokenUseCase validateTokenUseCase;

    @Operation(
            summary = "Emite access token e refresh token",
            description = """
                    Endpoint de autenticação baseado em usuário/senha.
                    Segue o fluxo semelhante ao OAuth2 Password/ROPC, retornando access_token e refresh_token.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token emitido com sucesso",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))),
            @ApiResponse(responseCode = "400", description = "Requisição inválida (dados faltando ou inválidos)"),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas"),
            @ApiResponse(responseCode = "429", description = "Muitas tentativas (rate limit)"),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    @PostMapping(
            path = "/token",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<TokenResponse> token(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest) {

        String clientIp = resolveClientIp(httpRequest);

        log.debug("[AuthController] - [token] -> Autenticando usuário username={} clientId={} ip={}",
                request.username(), request.clientId(), clientIp);

        var result = authenticateUserUseCase.authenticate(
                new AuthenticateUserUseCase.AuthenticationCommand(
                        request.clientId(),
                        request.clientSecret(),
                        request.username(),
                        request.password(),
                        request.scope(),
                        clientIp
                )
        );

        log.debug("[AuthController] - [token] -> Token emitido com sucesso username={} clientId={}",
                request.username(), request.clientId());

        TokenResponse response = new TokenResponse(
                result.tokenType(),
                result.accessToken().value(),
                result.refreshToken(),
                result.expiresInSeconds()
        );

        return ResponseEntity
                .ok()
                .headers(oauthNoStoreHeaders())
                .body(response);
    }

    @Operation(
            summary = "Renova access token usando refresh token",
            description = """
                    Endpoint para renovação do access token.
                    Recebe refresh_token válido e retorna novo access_token (e opcionalmente novo refresh_token).
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token renovado com sucesso",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))),
            @ApiResponse(responseCode = "400", description = "Refresh token inválido"),
            @ApiResponse(responseCode = "401", description = "Refresh token expirado ou revogado"),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    @PostMapping(
            path = "/refresh",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {

        log.debug("[AuthController] - [refresh] -> Renovando token clientId={}", request.clientId());

        var result = refreshTokenUseCase.refresh(
                new RefreshTokenUseCase.RefreshTokenCommand(
                        request.clientId(),
                        request.refreshToken(),
                        request.scope()
                )
        );

        log.debug("[AuthController] - [refresh] -> Token renovado com sucesso clientId={}", request.clientId());

        TokenResponse response = new TokenResponse(
                "Bearer",
                result.accessToken(),
                result.refreshToken(),
                result.expiresInSeconds()
        );

        return ResponseEntity
                .ok()
                .headers(oauthNoStoreHeaders())
                .body(response);
    }

    @Operation(
            summary = "Revoga um token (logout)",
            description = """
                    Revoga um token emitido pelo ms-auth.
                    Atualmente trata principalmente refresh tokens (logout).
                    
                    Em geral, o cliente deve enviar o refresh_token para encerrar a sessão.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Token revogado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Requisição inválida (token ausente ou malformado)"),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    @PostMapping(
            path = "/revoke",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Void> revoke(@Valid @RequestBody RevokeTokenRequest request) {

        boolean isRefresh = request.refreshToken() == null || request.refreshToken();

        log.debug(
                "[AuthController] - [revoke] -> Revogando token (refreshToken={})",
                isRefresh
        );

        revokeTokenUseCase.revoke(
                new RevokeTokenUseCase.RevokeTokenCommand(
                        request.token(),
                        isRefresh
                )
        );

        log.debug("[AuthController] - [revoke] -> Token revogado com sucesso");

        return ResponseEntity
                .noContent()
                .headers(oauthNoStoreHeaders())
                .build();
    }

    @Operation(
            summary = "Valida um access token JWT",
            description = """
                Valida a assinatura e a expiração de um token JWT.
                Retorna os claims principais caso o token seja válido.
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token válido",
                    content = @Content(schema = @Schema(implementation = ValidateTokenResponse.class))),
            @ApiResponse(responseCode = "400", description = "Token inválido ou malformado"),
            @ApiResponse(responseCode = "401", description = "Token expirado ou inválido"),
            @ApiResponse(responseCode = "500", description = "Erro interno no servidor")
    })
    @PostMapping(
            path = "/validate",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ValidateTokenResponse> validate(@Valid @RequestBody ValidateTokenRequest request) {

        log.debug("[AuthController] - [validate] -> Validando token");

        var claims = validateTokenUseCase.validate(request.token());

        var response = new ValidateTokenResponse(true, claims);

        return ResponseEntity
                .ok()
                .headers(oauthNoStoreHeaders())
                .body(response);
    }


    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        return request.getRemoteAddr();
    }

    private HttpHeaders oauthNoStoreHeaders() {
        HttpHeaders headers = new HttpHeaders();
        CacheControl.noStore()
                .mustRevalidate()
                .cachePrivate();
        headers.setCacheControl(
                CacheControl
                        .maxAge(Duration.ZERO)
        );
        headers.add("Pragma", "no-cache");
        return headers;
    }

}

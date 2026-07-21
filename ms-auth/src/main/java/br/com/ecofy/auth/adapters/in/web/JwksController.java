package br.com.ecofy.auth.adapters.in.web;

import br.com.ecofy.auth.core.port.in.GetJwksUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Disponibiliza as chaves públicas utilizadas na validação dos tokens JWT.
@RestController
@RequestMapping(path = "/.well-known", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@Tag(name = "JWKS", description = "JWKS endpoint para validação de tokens JWT pelos consumidores")
@Slf4j
@RequiredArgsConstructor
public class JwksController {

    private final GetJwksUseCase getJwksUseCase;

    // Retorna o documento JWKS com os headers de cache configurados.
    @Operation(
            summary = "Retorna o documento JWKS",
            description = """
                    Endpoint padrão OIDC/JWT para exposição das chaves públicas de assinatura.
                    Consumidores (API Gateway, outros microserviços) usam esse documento para validar tokens.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "JWKS retornado com sucesso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map.class)
                    )
            ),
            @ApiResponse(responseCode = "500", description = "Erro interno ao montar JWKS")
    })
    @GetMapping("/jwks.json")
    public ResponseEntity<Map<String, Object>> jwks() {
        Map<String, Object> jwks = getJwksUseCase.getJwks();

        int keysCount = extractKeysCount(jwks);

        log.debug(
                "[JwksController] - [jwks] -> Retornando JWKS keysCount={}",
                keysCount
        );

        return ResponseEntity
                .ok()
                .headers(jwksCacheHeaders())
                .body(jwks);
    }

    private int extractKeysCount(Map<String, Object> jwks) {
        Object keys = jwks.get("keys");

        if (keys instanceof Collection<?> collection) {
            return collection.size();
        }

        return 0;
    }

    // Configura o cache público aplicado às respostas do documento JWKS.
    private HttpHeaders jwksCacheHeaders() {
        HttpHeaders headers = new HttpHeaders();

        headers.setCacheControl(
                CacheControl.maxAge(Duration.ofMinutes(5))
                        .cachePublic()
        );

        return headers;
    }
}

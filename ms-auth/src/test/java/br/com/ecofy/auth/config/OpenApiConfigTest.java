package br.com.ecofy.auth.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springdoc.core.models.GroupedOpenApi;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Testes unitários da configuração OpenAPI")
class OpenApiConfigTest {

    private static final String SECURITY_SCHEME_BEARER = "BearerAuth";

    @Test
    @DisplayName("Deve criar a documentação principal com informações, links e autenticação JWT")
    void ecofyAuthOpenAPI_configuracaoPadrao_deveRetornarDocumentacaoCompleta() {
        // Arrange
        OpenApiConfig config = new OpenApiConfig();

        // Act
        OpenAPI result = config.ecofyAuthOpenAPI();

        // Assert
        assertNotNull(result);

        Info info = result.getInfo();
        Contact contact = info.getContact();
        License license = info.getLicense();
        ExternalDocumentation externalDocs = result.getExternalDocs();

        SecurityScheme securityScheme = result.getComponents()
                .getSecuritySchemes()
                .get(SECURITY_SCHEME_BEARER);

        SecurityRequirement securityRequirement = result.getSecurity()
                .get(0);

        assertAll(
                () -> assertNotNull(info),
                () -> assertEquals(
                        "EcoFy Auth Service – OIDC/JWT",
                        info.getTitle()
                ),
                () -> assertEquals(
                        """
                                Microserviço de autenticação da plataforma EcoFy.
                                Responsável por registro de usuários, OIDC/JWT, refresh tokens,
                                confirmação de e-mail e provisionamento de client applications.
                                """,
                        info.getDescription()
                ),
                () -> assertEquals("v1.0.0", info.getVersion()),
                () -> assertEquals(
                        "https://ecofy.com/terms",
                        info.getTermsOfService()
                ),
                () -> assertNotNull(contact),
                () -> assertEquals(
                        "EcoFy Platform",
                        contact.getName()
                ),
                () -> assertEquals(
                        "dev@ecofy.com",
                        contact.getEmail()
                ),
                () -> assertEquals(
                        "https://ecofy.com",
                        contact.getUrl()
                ),
                () -> assertNotNull(license),
                () -> assertEquals(
                        "Proprietary / EcoFy",
                        license.getName()
                ),
                () -> assertEquals(
                        "https://ecofy.com/license",
                        license.getUrl()
                ),
                () -> assertNotNull(externalDocs),
                () -> assertEquals(
                        "EcoFy Platform – Docs",
                        externalDocs.getDescription()
                ),
                () -> assertEquals(
                        "https://docs.ecofy.com",
                        externalDocs.getUrl()
                ),
                () -> assertNotNull(result.getComponents()),
                () -> assertNotNull(
                        result.getComponents().getSecuritySchemes()
                ),
                () -> assertNotNull(securityScheme),
                () -> assertEquals(
                        "Authorization",
                        securityScheme.getName()
                ),
                () -> assertEquals(
                        SecurityScheme.Type.HTTP,
                        securityScheme.getType()
                ),
                () -> assertEquals(
                        "bearer",
                        securityScheme.getScheme()
                ),
                () -> assertEquals(
                        "JWT",
                        securityScheme.getBearerFormat()
                ),
                () -> assertEquals(
                        """
                                Use: "Bearer &lt;token&gt;" no header Authorization.
                                Os tokens são emitidos pelo ms-auth e validados via JWKS.
                                """,
                        securityScheme.getDescription()
                ),
                () -> assertNotNull(result.getSecurity()),
                () -> assertEquals(1, result.getSecurity().size()),
                () -> assertTrue(
                        securityRequirement.containsKey(
                                SECURITY_SCHEME_BEARER
                        )
                ),
                () -> assertEquals(
                        List.of(),
                        securityRequirement.get(
                                SECURITY_SCHEME_BEARER
                        )
                )
        );
    }

    @Test
    @DisplayName("Deve agrupar os endpoints públicos da API e excluir os endpoints operacionais")
    void authApiGroup_configuracaoPadrao_deveAgruparEndpointsPublicos() {
        // Arrange
        OpenApiConfig config = new OpenApiConfig();

        // Act
        GroupedOpenApi result = config.authApiGroup();

        // Assert
        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(
                        "auth-api",
                        result.getGroup()
                ),
                () -> assertEquals(
                        List.of("/api/**"),
                        result.getPathsToMatch()
                ),
                () -> assertEquals(
                        List.of("/actuator/**"),
                        result.getPathsToExclude()
                )
        );
    }

    @Test
    @DisplayName("Deve agrupar separadamente os endpoints operacionais do Actuator")
    void actuatorGroup_configuracaoPadrao_deveAgruparEndpointsOperacionais() {
        // Arrange
        OpenApiConfig config = new OpenApiConfig();

        // Act
        GroupedOpenApi result = config.actuatorGroup();

        // Assert
        assertAll(
                () -> assertNotNull(result),
                () -> assertEquals(
                        "actuator",
                        result.getGroup()
                ),
                () -> assertEquals(
                        List.of("/actuator/**"),
                        result.getPathsToMatch()
                )
        );
    }
}

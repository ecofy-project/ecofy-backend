package br.com.ecofy.auth.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.*;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Configura a documentação OpenAPI e os grupos de endpoints do serviço.
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_BEARER = "BearerAuth";

    // Registra a documentação principal e o esquema de autenticação JWT.
    @Bean
    public OpenAPI ecofyAuthOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .externalDocs(externalDocs())
                .components(
                        new Components().addSecuritySchemes(
                                SECURITY_SCHEME_BEARER,
                                bearerSecurityScheme()
                        )
                )
                .addSecurityItem(
                        new SecurityRequirement().addList(
                                SECURITY_SCHEME_BEARER
                        )
                );
    }

    private Info apiInfo() {
        return new Info()
                .title("EcoFy Auth Service – OIDC/JWT")
                .description("""
                        Microserviço de autenticação da plataforma EcoFy.
                        Responsável por registro de usuários, OIDC/JWT, refresh tokens,
                        confirmação de e-mail e provisionamento de client applications.
                        """)
                .version("v1.0.0")
                .contact(
                        new Contact()
                                .name("EcoFy Platform")
                                .email("dev@ecofy.com")
                                .url("https://ecofy.com")
                )
                .license(
                        new License()
                                .name("Proprietary / EcoFy")
                                .url("https://ecofy.com/license")
                )
                .termsOfService("https://ecofy.com/terms");
    }

    private ExternalDocumentation externalDocs() {
        return new ExternalDocumentation()
                .description("EcoFy Platform – Docs")
                .url("https://docs.ecofy.com");
    }

    private SecurityScheme bearerSecurityScheme() {
        return new SecurityScheme()
                .name("Authorization")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("""
                        Use: "Bearer &lt;token&gt;" no header Authorization.
                        Os tokens são emitidos pelo ms-auth e validados via JWKS.
                        """);
    }

    // Agrupa os endpoints públicos da API e exclui os recursos operacionais.
    @Bean
    public GroupedOpenApi authApiGroup() {
        return GroupedOpenApi.builder()
                .group("auth-api")
                .pathsToMatch("/api/**")
                .pathsToExclude("/actuator/**")
                .build();
    }

    // Agrupa separadamente os endpoints operacionais do Actuator.
    @Bean
    public GroupedOpenApi actuatorGroup() {
        return GroupedOpenApi.builder()
                .group("actuator")
                .pathsToMatch("/actuator/**")
                .build();
    }
}

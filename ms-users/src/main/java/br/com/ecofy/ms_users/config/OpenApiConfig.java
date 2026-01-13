package br.com.ecofy.ms_users.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_BEARER = "BearerAuth";

    @Bean
    public OpenAPI ecofyUsersOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .externalDocs(externalDocs())
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_BEARER, bearerSecurityScheme())
                )
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_BEARER));
    }

    private Info apiInfo() {
        return new Info()
                .title("EcoFy Users Service – Profiles, Connections, Preferences & Accounts")
                .description("""
                        Microserviço de usuários da plataforma EcoFy.
                        Responsável por:
                        - criar e manter perfis de usuário (EcoUserProfile) e dados de contato;
                        - gerenciar conexões (connections) com provedores (ex.: BANK_API, OPEN_FINANCE, CSV_IMPORT, MANUAL);
                        - persistir preferências do usuário (UserPreference) e expor endpoints de leitura/atualização;
                        - gerenciar contas vinculadas (LinkedAccount) e relacionamento com provedores externos;
                        - servir como fonte de resolução de contato para notificações (ms-notification) e integrações.
                                                                     
                        Observações:
                        - A API é protegida via JWT (ms-auth) e normalmente acessada via API Gateway.
                        - Para operações mutáveis, recomenda-se Idempotency-Key quando aplicável.
                        - Em ambiente local, endpoints podem operar em modo permit-all via property (quando configurado).
                        """)
                .version("v1.0.0")
                .contact(new Contact()
                        .name("EcoFy Platform")
                        .email("dev@ecofy.com")
                        .url("https://ecofy.com"))
                .license(new License()
                        .name("Proprietary / EcoFy")
                        .url("https://ecofy.com/license"))
                .termsOfService("https://ecofy.com/terms");
    }

    private ExternalDocumentation externalDocs() {
        return new ExternalDocumentation()
                .description("EcoFy Platform – Documentação de Users")
                .url("https://docs.ecofy.com/users");
    }

    private SecurityScheme bearerSecurityScheme() {
        return new SecurityScheme()
                .name("Authorization")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("""
                        Use: "Bearer &lt;token&gt;" no header Authorization.
                        Tokens são emitidos pelo ms-auth e validados via JWKS.
                        """);
    }

    @Bean
    public GroupedOpenApi usersApiGroup() {
        return GroupedOpenApi.builder()
                .group("users-api")
                .pathsToMatch("/api/**")
                .pathsToExclude("/actuator/**")
                .build();
    }

    @Bean
    public GroupedOpenApi actuatorGroup() {
        return GroupedOpenApi.builder()
                .group("actuator")
                .pathsToMatch("/actuator/**")
                .build();
    }
}

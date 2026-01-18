package br.com.ecofy.ms_budgeting.config;

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

    // Registra o OpenAPI principal com metadados, docs externas e esquema de segurança JWT.
    @Bean
    public OpenAPI ecofyBudgetingOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .externalDocs(externalDocs())
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_BEARER, bearerSecurityScheme())
                )
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_BEARER));
    }

    // Monta as informações (title/description/version/contato/licença) exibidas no Swagger UI.
    private Info apiInfo() {
        return new Info()
                .title("EcoFy Budgeting Service – Budgets, Consumption & Alerts")
                .description("""
                        Microserviço de budgeting da plataforma EcoFy.
                        Responsável por:
                        - criar e manter budgets por usuário/categoria e período (mensal/semanal/custom);
                        - consolidar consumo por período a partir de transações categorizadas;
                        - gerar alertas (ex.: thresholds/over-budget) e publicar eventos para serviços downstream;
                        - expor endpoints para consulta de budgets e visão geral (overview).
                        
                        Observações:
                        - A API é protegida via JWT (ms-auth) e normalmente acessada via API Gateway.
                        - Para operações mutáveis, recomenda-se Idempotency-Key quando aplicável.
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

    // Define o link de documentação externa do serviço para ser exibido no OpenAPI.
    private ExternalDocumentation externalDocs() {
        return new ExternalDocumentation()
                .description("EcoFy Platform – Documentação de Budgeting")
                .url("https://docs.ecofy.com/budgeting");
    }

    // Define o SecurityScheme HTTP Bearer (JWT) para autenticação via header Authorization.
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

    // Agrupa endpoints de negócio (/api/**) em um grupo no Swagger, excluindo actuator.
    @Bean
    public GroupedOpenApi budgetingApiGroup() {
        return GroupedOpenApi.builder()
                .group("budgeting-api")
                .pathsToMatch("/api/**")
                .pathsToExclude("/actuator/**")
                .build();
    }

    // Agrupa endpoints do actuator (/actuator/**) em um grupo separado no Swagger.
    @Bean
    public GroupedOpenApi actuatorGroup() {
        return GroupedOpenApi.builder()
                .group("actuator")
                .pathsToMatch("/actuator/**")
                .build();
    }
}

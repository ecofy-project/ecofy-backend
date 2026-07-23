package br.com.ecofy.ms_insights.config;

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

// Configura a documentação OpenAPI do serviço de insights.
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_BEARER = "BearerAuth";

    // Configura os metadados e o esquema de autenticação JWT da API.
    @Bean
    public OpenAPI ecofyInsightsOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .externalDocs(externalDocs())
                .components(new Components()
                        .addSecuritySchemes(
                                SECURITY_SCHEME_BEARER,
                                bearerSecurityScheme()
                        )
                )
                .addSecurityItem(
                        new SecurityRequirement()
                                .addList(SECURITY_SCHEME_BEARER)
                );
    }

    private Info apiInfo() {
        return new Info()
                .title("EcoFy Insights Service – Insights, Metrics, Trends & Goals")
                .description("""
                        Microserviço de insights da plataforma EcoFy.
                        Responsável por:
                        - gerar insights (ex.: spending breakdown, cashflow, anomalies) a partir de transações categorizadas e budgets;
                        - consolidar métricas (snapshots) e séries temporais (trends) por usuário e período;
                        - gerenciar objetivos (goals) e disponibilizar bundles para dashboard;
                        - publicar eventos downstream (ex.: insight.created) quando aplicável.
                        
                        Observações:
                        - A API é protegida via JWT (ms-auth) e normalmente acessada via API Gateway.
                        - Para endpoints mutáveis e/ou geração sob demanda, recomenda-se uso de Idempotency-Key quando aplicável.
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
                .description("EcoFy Platform – Documentação de Insights")
                .url("https://docs.ecofy.com/insights");
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

    // Agrupa os endpoints públicos da API de insights.
    @Bean
    public GroupedOpenApi insightsApiGroup() {
        return GroupedOpenApi.builder()
                .group("insights-api")
                .pathsToMatch("/api/**")
                .pathsToExclude("/actuator/**")
                .build();
    }

    // Agrupa os endpoints operacionais do Actuator.
    @Bean
    public GroupedOpenApi actuatorGroup() {
        return GroupedOpenApi.builder()
                .group("actuator")
                .pathsToMatch("/actuator/**")
                .build();
    }
}

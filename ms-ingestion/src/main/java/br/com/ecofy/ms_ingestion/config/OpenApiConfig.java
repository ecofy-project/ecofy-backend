package br.com.ecofy.ms_ingestion.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.*;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_BEARER = "BearerAuth";

    @Bean
    public OpenAPI ecofyIngestionOpenAPI() {
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
                .title("EcoFy Ingestion Service – CSV/OFX & Event Ingestion")
                .description("""
                        Microserviço de ingestão da plataforma EcoFy.
                        Responsável por:
                        - upload e armazenamento de arquivos CSV/OFX;
                        - criação e processamento de ImportJobs;
                        - registro de RawTransactions e ImportErrors;
                        - publicação de eventos para categorização e outros serviços downstream.
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
                .description("EcoFy Platform – Documentação de Ingestão")
                .url("https://docs.ecofy.com/ingestion");
    }

    private SecurityScheme bearerSecurityScheme() {
        return new SecurityScheme()
                .name("Authorization")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("""
                        Use: "Bearer &lt;token&gt;" no header Authorization.
                        Os tokens são emitidos pelo ms-auth e propagados via API Gateway.
                        """);
    }

    // Grupo principal para endpoints REST da ingestão (exclui actuator).
    @Bean
    public GroupedOpenApi ingestionApiGroup() {
        return GroupedOpenApi.builder()
                .group("ingestion-api")
                .pathsToMatch("/api/**")
                .pathsToExclude("/actuator/**")
                .build();
    }

    // Grupo separado para actuator, se quiser inspecionar health/metrics via Swagger.
    @Bean
    public GroupedOpenApi actuatorGroup() {
        return GroupedOpenApi.builder()
                .group("actuator")
                .pathsToMatch("/actuator/**")
                .build();
    }

}

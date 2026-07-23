package br.com.ecofy.ms_categorization.config;

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

// Configura a documentação OpenAPI do serviço de categorização.
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_BEARER = "BearerAuth";

    // Configura os metadados e a autenticação da API.
    @Bean
    public OpenAPI ecofyCategorizationOpenAPI() {
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
                .title("EcoFy Categorization Service – Rules & Suggestions")
                .description("""
                        Microserviço de categorização da plataforma EcoFy.
                        Responsável por:
                        - gerenciar categorias e regras de categorização (rule engine);
                        - avaliar transações recebidas (eventos ou chamadas internas) e calcular score;
                        - gerar sugestões e/ou aplicar categoria automaticamente conforme configuração;
                        - publicar eventos de "transaction categorized" e "categorization applied" para serviços downstream.
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
                .description("EcoFy Platform – Documentação de Categorização")
                .url("https://docs.ecofy.com/categorization");
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

    // Agrupa os endpoints funcionais da API de categorização.
    @Bean
    public GroupedOpenApi categorizationApiGroup() {
        return GroupedOpenApi.builder()
                .group("categorization-api")
                .pathsToMatch("/api/**")
                .pathsToExclude("/actuator/**")
                .build();
    }

    // Agrupa os endpoints de monitoramento em documentação separada.
    @Bean
    public GroupedOpenApi actuatorGroup() {
        return GroupedOpenApi.builder()
                .group("actuator")
                .pathsToMatch("/actuator/**")
                .build();
    }
}

package br.com.ecofy.ms_notification.config;

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
    public OpenAPI ecofyNotificationOpenAPI() {
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
                .title("EcoFy Notification Service – Multichannel Notifications & Templates")
                .description("""
                        Microserviço de notificações multicanal da plataforma EcoFy.
                        Responsável por:
                        - receber eventos de domínio (ex.: budget.alert, insight.created) e gerar notificações;
                        - enviar notificações por canais (EMAIL / WHATSAPP / PUSH) via providers (adapters);
                        - manter templates por evento/canal (com fallback global e override por usuário);
                        - persistir notificações e tentativas de entrega (delivery attempts) em MongoDB;
                        - publicar eventos downstream (ex.: notification.sent) para observabilidade/auditoria.
                        
                        Observações:
                        - A API é protegida via JWT (ms-auth) e normalmente acessada via API Gateway.
                        - Para operações mutáveis, recomenda-se Idempotency-Key quando aplicável.
                        - Alguns providers podem operar em modo stub (ambiente dev/local).
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
                .description("EcoFy Platform – Documentação de Notificações")
                .url("https://docs.ecofy.com/notification");
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
    public GroupedOpenApi notificationApiGroup() {
        return GroupedOpenApi.builder()
                .group("notification-api")
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

package br.com.ecofy.ms_budgeting.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        classes = {
                SecurityConfig.class,
                SecurityConfigTest.TestController.class,
                SecurityConfigTest.TestCorsConfig.class,
                SecurityConfigTest.TestJwtConfig.class
        },
        properties = "ecofy.budgeting.security.permit-all=true"
)
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Test
    void shouldCreateSecurityConfigInstance() {
        SecurityConfig config = new SecurityConfig();

        assertNotNull(config);
    }

    @Test
    void shouldExposeSecurityFilterChainBean() {
        assertNotNull(securityFilterChain);
    }

    @Test
    void shouldPermitActuatorHealthWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("health-ok"));
    }

    @Test
    void shouldPermitActuatorInfoWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(content().string("info-ok"));
    }

    @Test
    void shouldPermitActuatorPrometheusWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string("prometheus-ok"));
    }

    @Test
    void shouldPermitOpenApiDocsWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/v3/api-docs/test"))
                .andExpect(status().isOk())
                .andExpect(content().string("openapi-ok"));
    }

    @Test
    void shouldPermitSwaggerUiWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string("swagger-ui-ok"));
    }

    @Test
    void shouldPermitSwaggerUiHtmlWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().isOk())
                .andExpect(content().string("swagger-ui-html-ok"));
    }

    @Test
    void shouldPermitBudgetingApiWithoutAuthenticationInLocalDevMode() throws Exception {
        mockMvc.perform(get("/api/budgeting/test"))
                .andExpect(status().isOk())
                .andExpect(content().string("budgeting-ok"));
    }

    @Test
    void shouldDisableCsrfForStatelessApi() throws Exception {
        mockMvc.perform(post("/api/budgeting/test"))
                .andExpect(status().isOk())
                .andExpect(content().string("budgeting-post-ok"));
    }

    @Test
    void shouldRequireAuthenticationForAnyOtherEndpoint() throws Exception {
        mockMvc.perform(get("/private/test"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void shouldAllowAuthenticatedUserForAnyOtherEndpoint() throws Exception {
        mockMvc.perform(get("/private/test"))
                .andExpect(status().isOk())
                .andExpect(content().string("private-ok"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturnForbiddenWhenAuthenticatedUserHasNoRequiredRole() throws Exception {
        mockMvc.perform(get("/admin/test"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAllowMethodSecuredEndpointWhenUserHasRequiredRole() throws Exception {
        mockMvc.perform(get("/admin/test"))
                .andExpect(status().isOk())
                .andExpect(content().string("admin-ok"));
    }

    @Test
    void shouldApplySameOriginFrameOptionsHeader() throws Exception {
        mockMvc.perform(get("/api/budgeting/test"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Frame-Options", "SAMEORIGIN"));
    }

    @Test
    void shouldApplyContentSecurityPolicyHeader() throws Exception {
        mockMvc.perform(get("/api/budgeting/test"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Content-Security-Policy",
                        "default-src 'self'"
                ));
    }

    @Test
    void shouldApplyNoReferrerPolicyHeader() throws Exception {
        mockMvc.perform(get("/api/budgeting/test"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Referrer-Policy",
                        "no-referrer"
                ));
    }

    @Test
    void shouldNotCreateHttpSessionForPublicRequest() throws Exception {
        mockMvc.perform(get("/api/budgeting/test"))
                .andExpect(status().isOk())
                .andExpect(result -> assertNull(result.getRequest().getSession(false)));
    }

    @Test
    void shouldHaveConfigurationAnnotation() {
        Configuration annotation =
                SecurityConfig.class.getAnnotation(Configuration.class);

        assertNotNull(annotation);
    }

    @Test
    void shouldHaveEnableMethodSecurityAnnotation() {
        EnableMethodSecurity annotation =
                SecurityConfig.class.getAnnotation(EnableMethodSecurity.class);

        assertNotNull(annotation);
    }

    @Test
    void shouldHaveBeanAnnotationOnSecurityFilterChainMethod() throws Exception {
        Method method = SecurityConfig.class.getDeclaredMethod(
                "securityFilterChain",
                HttpSecurity.class,
                org.springframework.core.env.Environment.class
        );

        Bean bean = method.getAnnotation(Bean.class);

        assertNotNull(bean);
        assertEquals(SecurityFilterChain.class, method.getReturnType());
    }

    @Test
    void shouldHaveExpectedPublicEndpoints() throws Exception {
        String[] publicEndpoints = readStringArrayConstant("PUBLIC_ENDPOINTS");

        assertArrayEquals(
                new String[]{
                        "/actuator/health",
                        "/actuator/info",
                        "/actuator/prometheus",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html"
                },
                publicEndpoints
        );
    }

    @Test
    void shouldHaveExpectedBudgetingApiEndpoints() throws Exception {
        String[] budgetingApiEndpoints =
                readStringArrayConstant("BUDGETING_API_ENDPOINTS");

        assertArrayEquals(
                new String[]{
                        "/api/budgeting/**"
                },
                budgetingApiEndpoints
        );
    }

    private static String[] readStringArrayConstant(String fieldName)
            throws Exception {
        Field field = SecurityConfig.class.getDeclaredField(fieldName);
        field.setAccessible(true);

        return (String[]) field.get(null);
    }

    @Configuration
    static class TestJwtConfig {

        // Stub de JwtDecoder para o oauth2ResourceServer.jwt() poder ser configurado no slice de teste
        // (nenhum teste envia bearer token real; permit-all/WithMockUser não o exercitam).
        @Bean
        org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder() {
            return token -> {
                throw new org.springframework.security.oauth2.jwt.BadJwtException("stub decoder");
            };
        }
    }

    @Configuration
    static class TestCorsConfig {

        @Bean
        CorsConfigurationSource corsConfigurationSource() {
            CorsConfiguration configuration = new CorsConfiguration();

            configuration.setAllowedOriginPatterns(List.of("*"));
            configuration.setAllowedMethods(List.of(
                    "GET",
                    "POST",
                    "PUT",
                    "PATCH",
                    "DELETE",
                    "OPTIONS"
            ));
            configuration.setAllowedHeaders(List.of("*"));
            configuration.setAllowCredentials(false);

            UrlBasedCorsConfigurationSource source =
                    new UrlBasedCorsConfigurationSource();

            source.registerCorsConfiguration("/**", configuration);

            return source;
        }
    }

    @RestController
    static class TestController {

        @GetMapping("/actuator/health")
        String health() {
            return "health-ok";
        }

        @GetMapping("/actuator/info")
        String info() {
            return "info-ok";
        }

        @GetMapping("/actuator/prometheus")
        String prometheus() {
            return "prometheus-ok";
        }

        @GetMapping("/v3/api-docs/test")
        String openApiDocs() {
            return "openapi-ok";
        }

        @GetMapping("/swagger-ui/index.html")
        String swaggerUi() {
            return "swagger-ui-ok";
        }

        @GetMapping("/swagger-ui.html")
        String swaggerUiHtml() {
            return "swagger-ui-html-ok";
        }

        @GetMapping("/api/budgeting/test")
        String budgetingGet() {
            return "budgeting-ok";
        }

        @PostMapping("/api/budgeting/test")
        String budgetingPost() {
            return "budgeting-post-ok";
        }

        @GetMapping("/private/test")
        String privateEndpoint() {
            return "private-ok";
        }

        @PreAuthorize("hasRole('ADMIN')")
        @GetMapping("/admin/test")
        String adminEndpoint() {
            return "admin-ok";
        }
    }
}
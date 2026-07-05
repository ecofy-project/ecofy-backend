package br.com.ecofy.ms_budgeting.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OpenApiConfigTest {

    private static final String SECURITY_SCHEME_BEARER = "BearerAuth";

    @Test
    void shouldCreateOpenApiConfigInstance() {
        OpenApiConfig config = new OpenApiConfig();

        assertNotNull(config);
    }

    @Test
    void shouldCreateEcofyBudgetingOpenAPI() {
        OpenApiConfig config = new OpenApiConfig();

        OpenAPI openAPI = config.ecofyBudgetingOpenAPI();

        assertNotNull(openAPI);
        assertNotNull(openAPI.getInfo());
        assertNotNull(openAPI.getExternalDocs());
        assertNotNull(openAPI.getComponents());
        assertNotNull(openAPI.getSecurity());
    }

    @Test
    void shouldConfigureOpenApiInfo() {
        OpenApiConfig config = new OpenApiConfig();

        OpenAPI openAPI = config.ecofyBudgetingOpenAPI();
        Info info = openAPI.getInfo();

        assertNotNull(info);
        assertEquals(
                "EcoFy Budgeting Service – Budgets, Consumption & Alerts",
                info.getTitle()
        );
        assertEquals("v1.0.0", info.getVersion());
        assertEquals("https://ecofy.com/terms", info.getTermsOfService());

        assertNotNull(info.getDescription());
        assertTrue(info.getDescription().contains("Microserviço de budgeting da plataforma EcoFy"));
        assertTrue(info.getDescription().contains("criar e manter budgets"));
        assertTrue(info.getDescription().contains("consolidar consumo por período"));
        assertTrue(info.getDescription().contains("gerar alertas"));
        assertTrue(info.getDescription().contains("JWT"));
        assertTrue(info.getDescription().contains("Idempotency-Key"));
    }

    @Test
    void shouldConfigureOpenApiContact() {
        OpenApiConfig config = new OpenApiConfig();

        OpenAPI openAPI = config.ecofyBudgetingOpenAPI();
        Contact contact = openAPI.getInfo().getContact();

        assertNotNull(contact);
        assertEquals("EcoFy Platform", contact.getName());
        assertEquals("dev@ecofy.com", contact.getEmail());
        assertEquals("https://ecofy.com", contact.getUrl());
    }

    @Test
    void shouldConfigureOpenApiLicense() {
        OpenApiConfig config = new OpenApiConfig();

        OpenAPI openAPI = config.ecofyBudgetingOpenAPI();
        License license = openAPI.getInfo().getLicense();

        assertNotNull(license);
        assertEquals("Proprietary / EcoFy", license.getName());
        assertEquals("https://ecofy.com/license", license.getUrl());
    }

    @Test
    void shouldConfigureExternalDocumentation() {
        OpenApiConfig config = new OpenApiConfig();

        OpenAPI openAPI = config.ecofyBudgetingOpenAPI();
        ExternalDocumentation externalDocs = openAPI.getExternalDocs();

        assertNotNull(externalDocs);
        assertEquals(
                "EcoFy Platform – Documentação de Budgeting",
                externalDocs.getDescription()
        );
        assertEquals("https://docs.ecofy.com/budgeting", externalDocs.getUrl());
    }

    @Test
    void shouldConfigureBearerSecurityScheme() {
        OpenApiConfig config = new OpenApiConfig();

        OpenAPI openAPI = config.ecofyBudgetingOpenAPI();
        Components components = openAPI.getComponents();

        assertNotNull(components);
        assertNotNull(components.getSecuritySchemes());
        assertTrue(components.getSecuritySchemes().containsKey(SECURITY_SCHEME_BEARER));

        SecurityScheme securityScheme =
                components.getSecuritySchemes().get(SECURITY_SCHEME_BEARER);

        assertNotNull(securityScheme);
        assertEquals("Authorization", securityScheme.getName());
        assertEquals(SecurityScheme.Type.HTTP, securityScheme.getType());
        assertEquals("bearer", securityScheme.getScheme());
        assertEquals("JWT", securityScheme.getBearerFormat());

        assertNotNull(securityScheme.getDescription());
        assertTrue(securityScheme.getDescription().contains("Bearer &lt;token&gt;"));
        assertTrue(securityScheme.getDescription().contains("ms-auth"));
        assertTrue(securityScheme.getDescription().contains("JWKS"));
    }

    @Test
    void shouldConfigureOpenApiSecurityRequirement() {
        OpenApiConfig config = new OpenApiConfig();

        OpenAPI openAPI = config.ecofyBudgetingOpenAPI();
        List<SecurityRequirement> securityRequirements = openAPI.getSecurity();

        assertNotNull(securityRequirements);
        assertEquals(1, securityRequirements.size());

        SecurityRequirement requirement = securityRequirements.get(0);

        assertTrue(requirement.containsKey(SECURITY_SCHEME_BEARER));
        assertNotNull(requirement.get(SECURITY_SCHEME_BEARER));
        assertTrue(requirement.get(SECURITY_SCHEME_BEARER).isEmpty());
    }

    @Test
    void shouldCreateBudgetingApiGroup() throws Exception {
        OpenApiConfig config = new OpenApiConfig();

        GroupedOpenApi group = config.budgetingApiGroup();

        assertNotNull(group);
        assertEquals("budgeting-api", readGroup(group));
        assertCollectionContains(readCollectionLike(group, "pathsToMatch"), "/api/**");
        assertCollectionContains(readCollectionLike(group, "pathsToExclude"), "/actuator/**");
    }

    @Test
    void shouldCreateActuatorGroup() throws Exception {
        OpenApiConfig config = new OpenApiConfig();

        GroupedOpenApi group = config.actuatorGroup();

        assertNotNull(group);
        assertEquals("actuator", readGroup(group));
        assertCollectionContains(readCollectionLike(group, "pathsToMatch"), "/actuator/**");

        Object pathsToExclude = readOptionalField(group, "pathsToExclude");

        if (pathsToExclude instanceof Collection<?> collection) {
            assertTrue(collection.isEmpty());
        }
    }

    @Test
    void shouldHaveConfigurationAnnotation() {
        Configuration annotation =
                OpenApiConfig.class.getAnnotation(Configuration.class);

        assertNotNull(annotation);
    }

    @Test
    void shouldHaveBeanAnnotationOnEcofyBudgetingOpenAPIMethod()
            throws Exception {
        Method method =
                OpenApiConfig.class.getDeclaredMethod("ecofyBudgetingOpenAPI");

        Bean bean = method.getAnnotation(Bean.class);

        assertNotNull(bean);
        assertEquals(OpenAPI.class, method.getReturnType());
    }

    @Test
    void shouldHaveBeanAnnotationOnBudgetingApiGroupMethod()
            throws Exception {
        Method method =
                OpenApiConfig.class.getDeclaredMethod("budgetingApiGroup");

        Bean bean = method.getAnnotation(Bean.class);

        assertNotNull(bean);
        assertEquals(GroupedOpenApi.class, method.getReturnType());
    }

    @Test
    void shouldHaveBeanAnnotationOnActuatorGroupMethod()
            throws Exception {
        Method method =
                OpenApiConfig.class.getDeclaredMethod("actuatorGroup");

        Bean bean = method.getAnnotation(Bean.class);

        assertNotNull(bean);
        assertEquals(GroupedOpenApi.class, method.getReturnType());
    }

    @Test
    void shouldInvokePrivateApiInfoMethod() throws Exception {
        OpenApiConfig config = new OpenApiConfig();

        Method method = OpenApiConfig.class.getDeclaredMethod("apiInfo");
        method.setAccessible(true);

        Info info = (Info) method.invoke(config);

        assertNotNull(info);
        assertEquals(
                "EcoFy Budgeting Service – Budgets, Consumption & Alerts",
                info.getTitle()
        );
        assertEquals("v1.0.0", info.getVersion());
        assertEquals("EcoFy Platform", info.getContact().getName());
        assertEquals("Proprietary / EcoFy", info.getLicense().getName());
    }

    @Test
    void shouldInvokePrivateExternalDocsMethod() throws Exception {
        OpenApiConfig config = new OpenApiConfig();

        Method method = OpenApiConfig.class.getDeclaredMethod("externalDocs");
        method.setAccessible(true);

        ExternalDocumentation externalDocs =
                (ExternalDocumentation) method.invoke(config);

        assertNotNull(externalDocs);
        assertEquals(
                "EcoFy Platform – Documentação de Budgeting",
                externalDocs.getDescription()
        );
        assertEquals("https://docs.ecofy.com/budgeting", externalDocs.getUrl());
    }

    @Test
    void shouldInvokePrivateBearerSecuritySchemeMethod() throws Exception {
        OpenApiConfig config = new OpenApiConfig();

        Method method = OpenApiConfig.class.getDeclaredMethod("bearerSecurityScheme");
        method.setAccessible(true);

        SecurityScheme securityScheme =
                (SecurityScheme) method.invoke(config);

        assertNotNull(securityScheme);
        assertEquals("Authorization", securityScheme.getName());
        assertEquals(SecurityScheme.Type.HTTP, securityScheme.getType());
        assertEquals("bearer", securityScheme.getScheme());
        assertEquals("JWT", securityScheme.getBearerFormat());
        assertTrue(securityScheme.getDescription().contains("Bearer &lt;token&gt;"));
    }

    @Test
    void shouldPrivateSecuritySchemeConstantHaveExpectedValue()
            throws Exception {
        Field field = OpenApiConfig.class.getDeclaredField("SECURITY_SCHEME_BEARER");
        field.setAccessible(true);

        assertEquals(SECURITY_SCHEME_BEARER, field.get(null));
    }

    private static String readGroup(GroupedOpenApi group) throws Exception {
        Object value = readAnyAccessorOrField(
                group,
                List.of("getGroup", "group"),
                "group"
        );

        assertNotNull(value);

        return value.toString();
    }

    private static Object readCollectionLike(
            GroupedOpenApi group,
            String fieldName
    ) throws Exception {
        return readAnyAccessorOrField(
                group,
                List.of(
                        "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1),
                        fieldName
                ),
                fieldName
        );
    }

    private static Object readAnyAccessorOrField(
            Object target,
            List<String> methodNames,
            String fieldName
    ) throws Exception {
        for (String methodName : methodNames) {
            try {
                Method method = target.getClass().getMethod(methodName);
                return method.invoke(target);
            } catch (NoSuchMethodException ignored) {
                // tenta próximo método
            }
        }

        return readFieldInHierarchy(target, fieldName);
    }

    private static Object readOptionalField(
            Object target,
            String fieldName
    ) throws Exception {
        try {
            return readFieldInHierarchy(target, fieldName);
        } catch (AssertionError ignored) {
            return null;
        }
    }

    private static Object readFieldInHierarchy(
            Object target,
            String fieldName
    ) throws Exception {
        Class<?> current = target.getClass();

        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }

        throw new AssertionError("Field not found in hierarchy: " + fieldName);
    }

    private static void assertCollectionContains(
            Object collectionLike,
            String expectedValue
    ) {
        assertNotNull(collectionLike);

        if (collectionLike instanceof Collection<?> collection) {
            assertTrue(collection.contains(expectedValue));
            return;
        }

        if (collectionLike instanceof String[] array) {
            assertTrue(List.of(array).contains(expectedValue));
            return;
        }

        if (collectionLike instanceof Object[] array) {
            assertTrue(List.of(array).contains(expectedValue));
            return;
        }

        if (collectionLike instanceof Map<?, ?> map) {
            assertTrue(map.containsKey(expectedValue) || map.containsValue(expectedValue));
            return;
        }

        assertTrue(collectionLike.toString().contains(expectedValue));
    }
}
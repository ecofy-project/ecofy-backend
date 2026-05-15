package br.com.ecofy.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;
import org.springdoc.core.models.GroupedOpenApi;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OpenApiConfigTest {

    @Test
    void ecofyAuthOpenAPI_shouldConfigureInfoExternalDocsSecuritySchemeAndRequirement() {
        OpenApiConfig config = new OpenApiConfig();

        OpenAPI api = config.ecofyAuthOpenAPI();

        assertNotNull(api);

        Info info = api.getInfo();
        assertNotNull(info);

        assertNotNull(info.getTitle());
        assertNotNull(info.getVersion());
        assertNotNull(info.getDescription());

        Contact contact = info.getContact();
        assertNotNull(contact);
        assertNotNull(contact.getName());
        assertNotNull(contact.getEmail());
        assertNotNull(contact.getUrl());

        License license = info.getLicense();
        assertNotNull(license);
        assertNotNull(license.getName());
        assertNotNull(license.getUrl());

        assertNotNull(info.getTermsOfService());

        assertNotNull(api.getExternalDocs());
        assertNotNull(api.getExternalDocs().getDescription());
        assertNotNull(api.getExternalDocs().getUrl());

        assertNotNull(api.getComponents());
        assertNotNull(api.getComponents().getSecuritySchemes());
        assertTrue(api.getComponents().getSecuritySchemes().containsKey("BearerAuth"));

        SecurityScheme scheme = api.getComponents().getSecuritySchemes().get("BearerAuth");
        assertNotNull(scheme);
        assertNotNull(scheme.getName());
        assertEquals(SecurityScheme.Type.HTTP, scheme.getType());
        assertNotNull(scheme.getScheme());
        assertNotNull(scheme.getBearerFormat());
        assertNotNull(scheme.getDescription());

        assertNotNull(api.getSecurity());
        assertFalse(api.getSecurity().isEmpty());
        assertTrue(api.getSecurity().get(0).containsKey("BearerAuth"));
    }

    @Test
    void authApiGroup_shouldMatchApiPathsAndExcludeActuator() throws Exception {
        OpenApiConfig config = new OpenApiConfig();

        GroupedOpenApi group = config.authApiGroup();

        assertNotNull(group);
        assertEquals("auth-api", group.getGroup());

        assertArrayEquals(new String[]{"/api/**"}, readPathsToMatch(group));
        assertArrayEquals(new String[]{"/actuator/**"}, readPathsToExclude(group));
    }

    @Test
    void actuatorGroup_shouldMatchActuatorPaths() throws Exception {
        OpenApiConfig config = new OpenApiConfig();

        GroupedOpenApi group = config.actuatorGroup();

        assertNotNull(group);
        assertEquals("actuator", group.getGroup());

        assertArrayEquals(new String[]{"/actuator/**"}, readPathsToMatch(group));
    }

    // heapers

    private static String[] readPathsToMatch(GroupedOpenApi group) throws Exception {
        return readStringArrayViaGetter(group, "getPathsToMatch");
    }

    private static String[] readPathsToExclude(GroupedOpenApi group) throws Exception {
        return readStringArrayViaGetter(group, "getPathsToExclude");
    }

    private static String[] readStringArrayViaGetter(Object target, String getterName) throws Exception {
        Method m = target.getClass().getMethod(getterName);
        Object v = m.invoke(target);

        if (v == null) return null;
        if (v instanceof String[] arr) return arr;
        if (v instanceof List<?> list) return list.stream().map(String::valueOf).toArray(String[]::new);

        throw new AssertionError("Unsupported return type for " + getterName + ": " + v.getClass().getName());
    }
}
package br.com.ecofy.auth.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MailConfigTest {

    @Test
    void ecofyMailProperties_shouldAllowOverrides_andDefaultsShouldBeNonBlank() {
        MailConfig.EcofyMailProperties p = new MailConfig.EcofyMailProperties();

        assertNotNull(p.getFrom());
        assertFalse(p.getFrom().isBlank());

        assertNotNull(p.getFrontendBaseUrl());
        assertFalse(p.getFrontendBaseUrl().isBlank());

        assertNotNull(p.getTemplatesBasePath());
        assertFalse(p.getTemplatesBasePath().isBlank());

        p.setFrom("support@ecofy.com");
        p.setFrontendBaseUrl("https://frontend.test");
        p.setTemplatesBasePath("classpath:/templates");

        assertEquals("support@ecofy.com", p.getFrom());
        assertEquals("https://frontend.test", p.getFrontendBaseUrl());
        assertEquals("classpath:/templates", p.getTemplatesBasePath());
    }

    @Test
    void mailConfigException_shouldStoreMessage() {
        MailConfig.MailConfigException ex = new MailConfig.MailConfigException("x");
        assertEquals("x", ex.getMessage());
    }
}
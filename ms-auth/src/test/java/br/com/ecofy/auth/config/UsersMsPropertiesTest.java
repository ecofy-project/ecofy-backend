package br.com.ecofy.auth.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UsersMsPropertiesTest {

    @Test
    void record_shouldExposeComponents_andSupportEqualityHashCodeToString() {
        UsersMsProperties p1 = new UsersMsProperties(true, "http://localhost:8081", "token-1");
        UsersMsProperties p2 = new UsersMsProperties(true, "http://localhost:8081", "token-1");
        UsersMsProperties p3 = new UsersMsProperties(false, "http://localhost:8082", "token-2");

        assertTrue(p1.enabled());
        assertEquals("http://localhost:8081", p1.baseUrl());
        assertEquals("token-1", p1.internalToken());

        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
        assertNotEquals(p1, p3);

        String s = p1.toString();
        assertNotNull(s);
        assertTrue(s.contains("enabled="));
        assertTrue(s.contains("baseUrl="));
        assertTrue(s.contains("internalToken="));
    }
}
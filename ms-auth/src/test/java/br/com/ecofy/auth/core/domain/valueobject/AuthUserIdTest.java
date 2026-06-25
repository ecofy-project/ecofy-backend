package br.com.ecofy.auth.core.domain.valueobject;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuthUserIdTest {

    @Test
    void shouldCreateAuthUserIdWithUuidValue() {
        UUID uuid = UUID.fromString("11111111-1111-1111-1111-111111111111");

        AuthUserId authUserId = new AuthUserId(uuid);

        assertEquals(uuid, authUserId.value());
        assertEquals("11111111-1111-1111-1111-111111111111", authUserId.toString());
    }

    @Test
    void shouldGenerateNewAuthUserId() {
        AuthUserId authUserId = AuthUserId.newId();

        assertNotNull(authUserId);
        assertNotNull(authUserId.value());
    }

    @Test
    void shouldGenerateDifferentIdsWhenCallingNewIdMultipleTimes() {
        AuthUserId first = AuthUserId.newId();
        AuthUserId second = AuthUserId.newId();

        assertNotEquals(first, second);
        assertNotEquals(first.value(), second.value());
    }

    @Test
    void shouldThrowExceptionWhenValueIsNull() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new AuthUserId(null)
        );

        assertEquals("value must not be null", exception.getMessage());
    }

    @Test
    void shouldCompareAuthUserIdByUuidValue() {
        UUID uuid = UUID.fromString("11111111-1111-1111-1111-111111111111");

        AuthUserId authUserId = new AuthUserId(uuid);
        AuthUserId sameValue = new AuthUserId(uuid);
        AuthUserId differentValue = new AuthUserId(
                UUID.fromString("22222222-2222-2222-2222-222222222222")
        );

        assertEquals(authUserId, authUserId);
        assertEquals(authUserId, sameValue);
        assertNotEquals(authUserId, differentValue);
        assertNotEquals(authUserId, null);
        assertNotEquals(authUserId, uuid);
        assertNotEquals(authUserId, "11111111-1111-1111-1111-111111111111");
    }

    @Test
    void shouldGenerateHashCodeUsingUuidValue() {
        UUID uuid = UUID.fromString("11111111-1111-1111-1111-111111111111");

        AuthUserId authUserId = new AuthUserId(uuid);
        AuthUserId sameValue = new AuthUserId(uuid);

        assertEquals(authUserId.hashCode(), sameValue.hashCode());
    }

    @Test
    void shouldBeSerializable() throws IOException, ClassNotFoundException {
        UUID uuid = UUID.fromString("11111111-1111-1111-1111-111111111111");
        AuthUserId original = new AuthUserId(uuid);

        byte[] serialized;

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {

            objectOutputStream.writeObject(original);
            serialized = byteArrayOutputStream.toByteArray();
        }

        AuthUserId deserialized;

        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(serialized);
             ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {

            deserialized = (AuthUserId) objectInputStream.readObject();
        }

        assertEquals(original, deserialized);
        assertEquals(original.value(), deserialized.value());
        assertEquals(original.toString(), deserialized.toString());
    }
}
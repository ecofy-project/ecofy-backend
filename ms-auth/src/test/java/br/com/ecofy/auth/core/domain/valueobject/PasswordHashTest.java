package br.com.ecofy.auth.core.domain.valueobject;

import org.junit.jupiter.api.Test;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

class PasswordHashTest {

    @Test
    void shouldCreatePasswordHashWithValue() {
        String hash = "$2a$10$abcdefghijklmnopqrstuv";

        PasswordHash passwordHash = new PasswordHash(hash);

        assertEquals(hash, passwordHash.value());
    }

    @Test
    void shouldThrowExceptionWhenValueIsNull() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new PasswordHash(null)
        );

        assertEquals("password hash must not be null", exception.getMessage());
    }

    @Test
    void shouldReturnMaskedValueInToString() {
        PasswordHash passwordHash = new PasswordHash("$2a$10$abcdefghijklmnopqrstuv");

        String result = passwordHash.toString();

        assertEquals("********", result);
        assertFalse(result.contains("$2a$10$abcdefghijklmnopqrstuv"));
    }

    @Test
    void shouldComparePasswordHashByValue() {
        PasswordHash passwordHash = new PasswordHash("hash-value");
        PasswordHash sameValue = new PasswordHash("hash-value");
        PasswordHash differentValue = new PasswordHash("another-hash-value");

        assertEquals(passwordHash, passwordHash);
        assertEquals(passwordHash, sameValue);
        assertNotEquals(passwordHash, differentValue);
        assertNotEquals(passwordHash, null);
        assertNotEquals(passwordHash, "hash-value");
    }

    @Test
    void shouldGenerateHashCodeUsingValue() {
        PasswordHash passwordHash = new PasswordHash("hash-value");
        PasswordHash sameValue = new PasswordHash("hash-value");

        assertEquals(passwordHash.hashCode(), sameValue.hashCode());
    }

    @Test
    void shouldBeSerializable() throws IOException, ClassNotFoundException {
        PasswordHash original = new PasswordHash("serialized-hash-value");

        byte[] serialized;

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {

            objectOutputStream.writeObject(original);
            serialized = byteArrayOutputStream.toByteArray();
        }

        PasswordHash deserialized;

        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(serialized);
             ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {

            deserialized = (PasswordHash) objectInputStream.readObject();
        }

        assertEquals(original, deserialized);
        assertEquals(original.value(), deserialized.value());
        assertEquals(original.hashCode(), deserialized.hashCode());
        assertEquals("********", deserialized.toString());
    }
}
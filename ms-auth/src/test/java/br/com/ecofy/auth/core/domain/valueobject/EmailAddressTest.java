package br.com.ecofy.auth.core.domain.valueobject;

import org.junit.jupiter.api.Test;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

class EmailAddressTest {

    @Test
    void shouldCreateEmailAddressWithNormalizedValue() {
        EmailAddress emailAddress = new EmailAddress("  MATHEUS.LEMES@ECOFY.COM.BR  ");

        assertEquals("matheus.lemes@ecofy.com.br", emailAddress.value());
        assertEquals("matheus.lemes@ecofy.com.br", emailAddress.toString());
    }

    @Test
    void shouldCreateEmailAddressWithValidSimpleEmail() {
        EmailAddress emailAddress = new EmailAddress("user@test.com");

        assertEquals("user@test.com", emailAddress.value());
    }

    @Test
    void shouldThrowExceptionWhenEmailIsNull() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new EmailAddress(null)
        );

        assertEquals("email must not be null", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenEmailIsBlank() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new EmailAddress("   ")
        );

        assertEquals("Invalid email address:    ", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenEmailDoesNotHaveAtSign() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new EmailAddress("matheus.ecofy.com")
        );

        assertEquals("Invalid email address: matheus.ecofy.com", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenEmailHasMoreThanOneAtSign() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new EmailAddress("matheus@@ecofy.com")
        );

        assertEquals("Invalid email address: matheus@@ecofy.com", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenEmailDoesNotHaveDomainDot() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new EmailAddress("matheus@ecofy")
        );

        assertEquals("Invalid email address: matheus@ecofy", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenEmailHasWhitespaceInside() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new EmailAddress("matheus lemes@ecofy.com")
        );

        assertEquals("Invalid email address: matheus lemes@ecofy.com", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenEmailStartsWithAtSign() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new EmailAddress("@ecofy.com")
        );

        assertEquals("Invalid email address: @ecofy.com", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenEmailEndsWithAtSign() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new EmailAddress("matheus@")
        );

        assertEquals("Invalid email address: matheus@", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenEmailDomainStartsWithDot() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new EmailAddress("matheus@.com")
        );

        assertEquals("Invalid email address: matheus@.com", exception.getMessage());
    }

    @Test
    void shouldCompareEmailAddressByNormalizedValue() {
        EmailAddress emailAddress = new EmailAddress("MATHEUS@ECOFY.COM");
        EmailAddress sameValue = new EmailAddress("  matheus@ecofy.com  ");
        EmailAddress differentValue = new EmailAddress("outro@ecofy.com");

        assertEquals(emailAddress, emailAddress);
        assertEquals(emailAddress, sameValue);
        assertNotEquals(emailAddress, differentValue);
        assertNotEquals(emailAddress, null);
        assertNotEquals(emailAddress, "matheus@ecofy.com");
    }

    @Test
    void shouldGenerateHashCodeUsingNormalizedValue() {
        EmailAddress emailAddress = new EmailAddress("MATHEUS@ECOFY.COM");
        EmailAddress sameValue = new EmailAddress("  matheus@ecofy.com  ");

        assertEquals(emailAddress.hashCode(), sameValue.hashCode());
    }

    @Test
    void shouldBeSerializable() throws IOException, ClassNotFoundException {
        EmailAddress original = new EmailAddress("MATHEUS@ECOFY.COM");

        byte[] serialized;

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {

            objectOutputStream.writeObject(original);
            serialized = byteArrayOutputStream.toByteArray();
        }

        EmailAddress deserialized;

        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(serialized);
             ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {

            deserialized = (EmailAddress) objectInputStream.readObject();
        }

        assertEquals(original, deserialized);
        assertEquals(original.value(), deserialized.value());
        assertEquals(original.toString(), deserialized.toString());
        assertEquals(original.hashCode(), deserialized.hashCode());
    }
}
package br.com.ecofy.ms_users.core.domain.valueobject;

import java.util.Objects;
import java.util.regex.Pattern;

public record EmailAddress(String value) {

    private static final Pattern BASIC = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public EmailAddress {
        if (value == null || value.isBlank() || !BASIC.matcher(value).matches()) {
            throw new IllegalArgumentException("invalid email");
        }
    }

    public static EmailAddress of(String v) {
        return new EmailAddress(Objects.requireNonNull(v, "email must not be null"));
    }
}

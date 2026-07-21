package br.com.ecofy.auth.core.domain.valueobject;

import java.io.Serializable;
import java.util.Objects;

// Representa o hash imutável de uma senha no domínio.
public final class PasswordHash implements Serializable {

    private final String value;

    public PasswordHash(String value) {
        this.value = Objects.requireNonNull(
                value,
                "password hash must not be null"
        );
    }

    public String value() {
        return value;
    }

    // Oculta o conteúdo do hash na representação textual.
    @Override
    public String toString() {
        return "********";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof PasswordHash that)) {
            return false;
        }

        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}

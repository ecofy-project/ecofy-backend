package br.com.ecofy.auth.core.domain.valueobject;

import java.io.Serializable;
import java.util.Objects;
import java.util.regex.Pattern;

// Value Object que representa um endereço de e-mail no domínio com normalização e validação básica de formato.
public final class EmailAddress implements Serializable {

    private static final Pattern SIMPLE_EMAIL_REGEX =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final String value;

    // Cria um EmailAddress normalizando (trim/lowercase) e validando o formato para garantir consistência no domínio.
    public EmailAddress(String value) {
        Objects.requireNonNull(value, "email must not be null");
        String trimmed = value.trim().toLowerCase();
        if (!SIMPLE_EMAIL_REGEX.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Invalid email address: " + value);
        }
        this.value = trimmed;
    }

    // Retorna o e-mail normalizado (lowercase) encapsulado pelo value object.
    public String value() {
        return value;
    }

    // Retorna o e-mail como String para logs e serialização simples.
    @Override
    public String toString() {
        return value;
    }

    // Compara EmailAddress por valor normalizado para igualdade semântica no domínio.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmailAddress that)) return false;
        return value.equals(that.value);
    }

    // Gera hash consistente com equals para uso em coleções e estruturas de dados.
    @Override
    public int hashCode() {
        return value.hashCode();
    }

}

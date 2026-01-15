package br.com.ecofy.auth.core.domain.valueobject;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

// Value Object que representa a identidade (UUID) do usuário no domínio, garantindo tipagem forte e imutabilidade.
public final class AuthUserId implements Serializable {

    private final UUID value;

    // Cria um AuthUserId a partir de um UUID existente, validando nulo.
    public AuthUserId(UUID value) {
        this.value = Objects.requireNonNull(value, "value must not be null");
    }

    // Gera um novo AuthUserId com UUID aleatório para criação de usuários no domínio.
    public static AuthUserId newId() {
        return new AuthUserId(UUID.randomUUID());
    }

    // Retorna o UUID encapsulado pelo value object.
    public UUID value() {
        return value;
    }

    // Retorna a representação textual do UUID para logs/serialização simples.
    @Override
    public String toString() {
        return value.toString();
    }

    // Compara AuthUserId por valor (UUID) para igualdade semântica no domínio.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuthUserId that)) return false;
        return Objects.equals(value, that.value);
    }

    // Gera hash consistente com equals para uso em coleções e estruturas de dados.
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

}

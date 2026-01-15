package br.com.ecofy.auth.core.domain.valueobject;

import java.io.Serializable;
import java.util.Objects;

// Value Object que encapsula o hash da senha para tipagem forte e evitar uso acidental como String comum.
public final class PasswordHash implements Serializable {

    private final String value;

    // Cria um PasswordHash a partir de um hash já calculado, garantindo que o valor não seja nulo.
    public PasswordHash(String value) {
        this.value = Objects.requireNonNull(value, "password hash must not be null");
    }

    // Retorna o hash encapsulado para persistência/validação (nunca a senha em texto puro).
    public String value() {
        return value;
    }

    // Oculta o valor real no toString para evitar vazamento de hash em logs.
    @Override
    public String toString() {
        return "********";
    }

    // Compara PasswordHash por valor para igualdade semântica em operações internas do domínio.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PasswordHash that)) return false;
        return value.equals(that.value);
    }

    // Gera hash consistente com equals para uso em coleções e estruturas de dados.
    @Override
    public int hashCode() {
        return value.hashCode();
    }

}

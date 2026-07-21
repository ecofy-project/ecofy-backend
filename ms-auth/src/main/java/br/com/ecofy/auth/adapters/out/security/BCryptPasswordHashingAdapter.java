package br.com.ecofy.auth.adapters.out.security;

import br.com.ecofy.auth.core.domain.valueobject.PasswordHash;
import br.com.ecofy.auth.core.port.out.PasswordHashingPort;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Centraliza a geração e a verificação segura de hashes de senha.
@Component
@Slf4j
public class BCryptPasswordHashingAdapter implements PasswordHashingPort {

    private final PasswordEncoder passwordEncoder;

    public BCryptPasswordHashingAdapter(
            PasswordEncoder passwordEncoder
    ) {
        this.passwordEncoder = Objects.requireNonNull(
                passwordEncoder,
                "passwordEncoder must not be null"
        );
    }

    // Converte a senha recebida em um hash protegido.
    @Override
    public PasswordHash hash(String rawPassword) {
        Objects.requireNonNull(
                rawPassword,
                "rawPassword must not be null"
        );

        if (rawPassword.isBlank()) {
            throw new IllegalArgumentException(
                    "rawPassword must not be blank"
            );
        }

        log.debug(
                "[BCryptPasswordHashingAdapter] - [hash] -> Gerando hash de senha (tamanho={})",
                rawPassword.length()
        );

        String encoded = passwordEncoder.encode(rawPassword);

        log.debug(
                "[BCryptPasswordHashingAdapter] - [hash] -> Hash gerado com sucesso"
        );

        return new PasswordHash(encoded);
    }

    // Valida a senha recebida contra o hash armazenado.
    @Override
    public boolean matches(
            String rawPassword,
            PasswordHash hash
    ) {
        Objects.requireNonNull(
                rawPassword,
                "rawPassword must not be null"
        );
        Objects.requireNonNull(
                hash,
                "hash must not be null"
        );

        log.debug(
                "[BCryptPasswordHashingAdapter] - [matches] -> Verificando senha (tamanho={})",
                rawPassword.length()
        );

        boolean matches = passwordEncoder.matches(
                rawPassword,
                hash.value()
        );

        log.debug(
                "[BCryptPasswordHashingAdapter] - [matches] -> Resultado matches={}",
                matches
        );

        return matches;
    }
}

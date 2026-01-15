package br.com.ecofy.auth.adapters.out.persistence;

import br.com.ecofy.auth.adapters.out.persistence.mapper.PersistenceMapper;
import br.com.ecofy.auth.adapters.out.persistence.repository.JwkKeyRepository;
import br.com.ecofy.auth.core.domain.JwkKey;
import br.com.ecofy.auth.core.port.out.JwksRepositoryPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Component
@Slf4j
public class JwksJpaAdapter implements JwksRepositoryPort {

    private final JwkKeyRepository repository;

    // Injeta o repositório JPA responsável por consultar as chaves JWK no banco.
    public JwksJpaAdapter(JwkKeyRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    // Busca no banco as chaves JWK ativas, mapeia para o domínio e retorna uma lista imutável.
    @Override
    @Transactional(readOnly = true)
    public List<JwkKey> findActiveSigningKeys() {
        log.debug("[JwksJpaAdapter] - [findActiveSigningKeys] -> Buscando JWKS ativos");

        var entities = repository.findByActiveTrue();

        if (entities == null || entities.isEmpty()) {
            log.debug("[JwksJpaAdapter] - [findActiveSigningKeys] -> Nenhuma JWK ativa encontrada");
            return Collections.emptyList();
        }

        List<JwkKey> keys = entities.stream()
                .map(PersistenceMapper::toDomain)
                .toList();

        log.debug(
                "[JwksJpaAdapter] - [findActiveSigningKeys] -> {} chaves ativas carregadas",
                keys.size()
        );

        return List.copyOf(keys);
    }

}


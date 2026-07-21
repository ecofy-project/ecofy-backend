package br.com.ecofy.auth.adapters.out.persistence;

import br.com.ecofy.auth.adapters.out.persistence.entity.RefreshTokenEntity;
import br.com.ecofy.auth.adapters.out.persistence.mapper.PersistenceMapper;
import br.com.ecofy.auth.adapters.out.persistence.repository.RefreshTokenRepository;
import br.com.ecofy.auth.core.domain.RefreshToken;
import br.com.ecofy.auth.core.port.out.RefreshTokenStorePort;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Centraliza a persistência, a consulta e a revogação de refresh tokens.
@Component
@Slf4j
public class RefreshTokenJpaAdapter implements RefreshTokenStorePort {

    private final RefreshTokenRepository repository;

    public RefreshTokenJpaAdapter(RefreshTokenRepository repository) {
        this.repository = Objects.requireNonNull(
                repository,
                "repository must not be null"
        );
    }

    // Persiste o refresh token e retorna o domínio atualizado.
    @Override
    @Transactional
    public RefreshToken save(RefreshToken token) {
        Objects.requireNonNull(token, "token must not be null");

        log.debug(
                "[RefreshTokenJpaAdapter] - [save] -> Salvando refreshToken id={} userId={} clientId={}",
                token.id(),
                token.userId().value(),
                token.clientId()
        );

        RefreshTokenEntity entity = PersistenceMapper.toEntity(token);
        RefreshTokenEntity saved = repository.save(entity);

        log.debug(
                "[RefreshTokenJpaAdapter] - [save] -> RefreshToken persistido id={} revoked={}",
                saved.getId(),
                saved.isRevoked()
        );

        return PersistenceMapper.toDomain(saved);
    }

    // Carrega o refresh token associado ao valor informado.
    @Override
    @Transactional(readOnly = true)
    public Optional<RefreshToken> findByTokenValue(String tokenValue) {
        Objects.requireNonNull(
                tokenValue,
                "tokenValue must not be null"
        );

        log.debug(
                "[RefreshTokenJpaAdapter] - [findByTokenValue] -> Buscando refreshToken tokenMask={}",
                maskToken(tokenValue)
        );

        return repository.findByTokenValue(tokenValue)
                .map(entity -> {
                    log.debug(
                            "[RefreshTokenJpaAdapter] - [findByTokenValue] -> RefreshToken encontrado id={} revoked={}",
                            entity.getId(),
                            entity.isRevoked()
                    );

                    return PersistenceMapper.toDomain(entity);
                });
    }

    // Revoga o refresh token quando ele existe e permanece ativo.
    @Override
    @Transactional
    public void revoke(String tokenValue) {
        Objects.requireNonNull(
                tokenValue,
                "tokenValue must not be null"
        );

        log.debug(
                "[RefreshTokenJpaAdapter] - [revoke] -> Revogando refreshToken tokenMask={}",
                maskToken(tokenValue)
        );

        repository.findByTokenValue(tokenValue).ifPresentOrElse(entity -> {
            if (!entity.isRevoked()) {
                entity.setRevoked(true);
                repository.save(entity);

                log.debug(
                        "[RefreshTokenJpaAdapter] - [revoke] -> RefreshToken revogado id={}",
                        entity.getId()
                );
            } else {
                log.debug(
                        "[RefreshTokenJpaAdapter] - [revoke] -> RefreshToken já estava revogado id={}",
                        entity.getId()
                );
            }
        }, () -> log.debug(
                "[RefreshTokenJpaAdapter] - [revoke] -> Nenhum refreshToken encontrado para tokenMask={}",
                maskToken(tokenValue)
        ));
    }

    private String maskToken(String token) {
        if (token == null || token.isBlank()) {
            return "***";
        }

        return token.length() > 12
                ? token.substring(0, 12) + "..."
                : "***";
    }
}

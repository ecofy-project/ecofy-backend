package br.com.ecofy.auth.adapters.out.persistence;

import br.com.ecofy.auth.adapters.out.persistence.entity.AuthUserEntity;
import br.com.ecofy.auth.adapters.out.persistence.entity.RoleEntity;
import br.com.ecofy.auth.adapters.out.persistence.mapper.PersistenceMapper;
import br.com.ecofy.auth.adapters.out.persistence.repository.AuthUserRepository;
import br.com.ecofy.auth.adapters.out.persistence.repository.RoleRepository;
import br.com.ecofy.auth.core.domain.AuthUser;
import br.com.ecofy.auth.core.domain.Role;
import br.com.ecofy.auth.core.domain.valueobject.AuthUserId;
import br.com.ecofy.auth.core.domain.valueobject.EmailAddress;
import br.com.ecofy.auth.core.port.out.LoadAuthUserByEmailPort;
import br.com.ecofy.auth.core.port.out.LoadAuthUserByIdPort;
import br.com.ecofy.auth.core.port.out.SaveAuthUserPort;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Centraliza a persistência e a consulta de usuários autenticáveis.
@Component
@Slf4j
public class AuthUserJpaAdapter
        implements SaveAuthUserPort, LoadAuthUserByEmailPort, LoadAuthUserByIdPort {

    private final AuthUserRepository authUserRepository;
    private final RoleRepository roleRepository;

    public AuthUserJpaAdapter(
            AuthUserRepository authUserRepository,
            RoleRepository roleRepository
    ) {
        this.authUserRepository = Objects.requireNonNull(
                authUserRepository,
                "authUserRepository must not be null"
        );
        this.roleRepository = Objects.requireNonNull(
                roleRepository,
                "roleRepository must not be null"
        );
    }

    // Persiste o usuário e atualiza seus dados temporais e relacionamentos.
    @Override
    @Transactional
    public AuthUser save(AuthUser user) {
        Objects.requireNonNull(user, "user must not be null");

        Instant now = Instant.now();
        String userId = user.id().value().toString();

        log.debug(
                "[AuthUserJpaAdapter] - [save] -> Persistindo usuário id={} email={}",
                userId,
                user.email().value()
        );

        AuthUserEntity entity = authUserRepository.findById(user.id().value())
                .map(existing -> {
                    log.debug(
                            "[AuthUserJpaAdapter] - [save] -> Atualizando usuário existente id={}",
                            userId
                    );

                    return existing;
                })
                .orElseGet(() -> {
                    log.debug(
                            "[AuthUserJpaAdapter] - [save] -> Criando novo usuário id={}",
                            userId
                    );

                    AuthUserEntity e = new AuthUserEntity();
                    e.setId(user.id().value());
                    e.setCreatedAt(now);

                    return e;
                });

        mapDomainToEntity(user, entity, now);

        AuthUserEntity saved = authUserRepository.save(entity);

        log.debug(
                "[AuthUserJpaAdapter] - [save] -> Usuário persistido com sucesso id={} email={}",
                saved.getId(),
                saved.getEmail()
        );

        return PersistenceMapper.toDomain(
                saved,
                saved.getRoles(),
                saved.getPermissions()
        );
    }

    // Carrega o usuário associado ao endereço de e-mail informado.
    @Override
    @Transactional(readOnly = true)
    public Optional<AuthUser> loadByEmail(EmailAddress email) {
        Objects.requireNonNull(email, "email must not be null");

        log.debug(
                "[AuthUserJpaAdapter] - [loadByEmail] -> Buscando usuário por email={}",
                email.value()
        );

        return authUserRepository.findByEmailIgnoreCase(email.value())
                .map(entity -> {
                    log.debug(
                            "[AuthUserJpaAdapter] - [loadByEmail] -> Usuário encontrado id={} email={}",
                            entity.getId(),
                            entity.getEmail()
                    );

                    return PersistenceMapper.toDomain(
                            entity,
                            entity.getRoles(),
                            entity.getPermissions()
                    );
                });
    }

    // Carrega o usuário associado ao identificador informado.
    @Override
    @Transactional(readOnly = true)
    public Optional<AuthUser> loadById(AuthUserId id) {
        Objects.requireNonNull(id, "id must not be null");

        log.debug(
                "[AuthUserJpaAdapter] - [loadById] -> Buscando usuário por id={}",
                id.value()
        );

        return authUserRepository.findById(id.value())
                .map(entity -> {
                    log.debug(
                            "[AuthUserJpaAdapter] - [loadById] -> Usuário encontrado id={} email={}",
                            entity.getId(),
                            entity.getEmail()
                    );

                    return PersistenceMapper.toDomain(
                            entity,
                            entity.getRoles(),
                            entity.getPermissions()
                    );
                });
    }

    // Converte os dados mutáveis do usuário para a entidade persistida.
    private void mapDomainToEntity(
            AuthUser user,
            AuthUserEntity entity,
            Instant now
    ) {
        entity.setEmail(user.email().value());
        entity.setPasswordHash(user.passwordHash().value());
        entity.setStatus(user.status());
        entity.setEmailVerified(user.isEmailVerified());
        entity.setFirstName(user.firstName());
        entity.setLastName(user.lastName());
        entity.setLocale(user.locale());
        entity.setLastLoginAt(user.lastLoginAt());
        entity.setFailedLoginAttempts(user.failedLoginAttempts());
        entity.setRoles(resolveRoleEntities(user));

        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }

        entity.setUpdatedAt(now);
    }

    // Resolve as funções do domínio para entidades existentes no banco.
    private Set<RoleEntity> resolveRoleEntities(AuthUser user) {
        Set<RoleEntity> roleEntities = new HashSet<>();

        for (Role role : user.roles()) {
            if (role == null || role.name() == null) {
                continue;
            }

            roleRepository.findById(role.name()).ifPresentOrElse(
                    roleEntities::add,
                    () -> log.warn(
                            "[AuthUserJpaAdapter] - [resolveRoleEntities] -> Role '{}' inexistente no banco; ignorada na persistência",
                            role.name()
                    )
            );
        }

        return roleEntities;
    }
}

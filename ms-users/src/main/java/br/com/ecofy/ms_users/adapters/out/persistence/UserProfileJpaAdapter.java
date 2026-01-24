package br.com.ecofy.ms_users.adapters.out.persistence;

import br.com.ecofy.ms_users.adapters.out.persistence.mapper.UserProfileMapper;
import br.com.ecofy.ms_users.adapters.out.persistence.repository.UserProfileRepository;
import br.com.ecofy.ms_users.core.domain.EcoUserProfile;
import br.com.ecofy.ms_users.core.port.out.LoadUserProfilePort;
import br.com.ecofy.ms_users.core.port.out.SaveUserProfilePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class UserProfileJpaAdapter implements SaveUserProfilePort, LoadUserProfilePort {

    private final UserProfileRepository repo;
    private final UserProfileMapper mapper;

    // Inicializa o adapter JPA de perfil de usuário com o repositório e o mapper (entity <-> domain).
    public UserProfileJpaAdapter(UserProfileRepository repo, UserProfileMapper mapper) {
        this.repo = Objects.requireNonNull(repo, "repo must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    // Persiste o EcoUserProfile via JPA e retorna o perfil salvo convertido para domínio.
    @Override
    @Transactional
    public EcoUserProfile save(EcoUserProfile profile) {
        Objects.requireNonNull(profile, "profile must not be null");
        Objects.requireNonNull(profile.getId(), "profile.id must not be null");

        log.info(
                "[UserProfileJpaAdapter] - [save] -> userId={} hasExternalAuthId={} hasEmail={} hasPhone={} status={}",
                profile.getId().value(),
                profile.getExternalAuthId() != null,
                profile.getEmail() != null,
                profile.getPhone() != null,
                profile.getStatus()
        );

        var saved = repo.save(mapper.toEntity(profile));

        log.debug(
                "[UserProfileJpaAdapter] - [save] -> saved userId={} updatedAt={}",
                saved.getId(),
                saved.getUpdatedAt()
        );

        return mapper.toDomain(saved);
    }

    // Busca um EcoUserProfile pelo userId (UUID) e retorna Optional com o domínio quando encontrado.
    @Override
    public Optional<EcoUserProfile> findById(UUID id) {
        Objects.requireNonNull(id, "id must not be null");

        log.debug("[UserProfileJpaAdapter] - [findById] -> userId={}", id);

        var found = repo.findById(id).map(mapper::toDomain);

        log.info(
                "[UserProfileJpaAdapter] - [findById] -> userId={} found={}",
                id,
                found.isPresent()
        );

        return found;
    }

    // Busca um EcoUserProfile pelo externalAuthId (ID do provedor de autenticação) e retorna Optional com o domínio quando encontrado.
    @Override
    public Optional<EcoUserProfile> findByExternalAuthId(String externalAuthId) {
        String ext = blankToNull(externalAuthId);
        if (ext == null) {
            log.debug("[UserProfileJpaAdapter] - [findByExternalAuthId] -> externalAuthId=<blank> found=false");
            return Optional.empty();
        }

        log.debug("[UserProfileJpaAdapter] - [findByExternalAuthId] -> externalAuthId={}", safeExtId(ext));

        var found = repo.findByExternalAuthId(ext).map(mapper::toDomain);

        log.info(
                "[UserProfileJpaAdapter] - [findByExternalAuthId] -> externalAuthId={} found={}",
                safeExtId(ext),
                found.isPresent()
        );

        return found;
    }

    // Normaliza string opcional, retornando null quando vazia/em branco e trimando quando presente.
    private static String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    // Mascara o externalAuthId para logging seguro, evitando expor o identificador completo.
    private static String safeExtId(String externalAuthId) {
        // Evita logar IDs externos completos (podem ser sensíveis dependendo do provedor).
        if (externalAuthId == null || externalAuthId.isBlank()) return "<empty>";
        if (externalAuthId.length() <= 8) return "***";
        return externalAuthId.substring(0, 4) + "..." + externalAuthId.substring(externalAuthId.length() - 4);
    }

}

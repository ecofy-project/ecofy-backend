package br.com.ecofy.ms_users.adapters.out.persistence;

import br.com.ecofy.ms_users.adapters.out.persistence.entity.UserPreferenceEntity;
import br.com.ecofy.ms_users.adapters.out.persistence.mapper.PreferenceMapper;
import br.com.ecofy.ms_users.adapters.out.persistence.repository.UserPreferenceRepository;
import br.com.ecofy.ms_users.core.domain.UserPreference;
import br.com.ecofy.ms_users.core.port.out.LoadUserPreferencesPort;
import br.com.ecofy.ms_users.core.port.out.SaveUserPreferencePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
public class UserPreferenceJpaAdapter implements SaveUserPreferencePort, LoadUserPreferencesPort {

    private final UserPreferenceRepository repo;
    private final PreferenceMapper mapper;

    public UserPreferenceJpaAdapter(UserPreferenceRepository repo, PreferenceMapper mapper) {
        this.repo = Objects.requireNonNull(repo, "repo must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    @Transactional
    public List<UserPreference> upsertAll(List<UserPreference> prefs) {
        if (prefs == null || prefs.isEmpty()) {
            log.debug("[UserPreferenceJpaAdapter] - [upsertAll] -> emptyInput=true");
            return List.of();
        }

        log.info(
                "[UserPreferenceJpaAdapter] - [upsertAll] -> requestedCount={}",
                prefs.size()
        );

        List<UserPreference> out = new ArrayList<>(prefs.size());

        for (var p : prefs) {
            Objects.requireNonNull(p, "preference must not be null");
            Objects.requireNonNull(p.getUserId(), "preference.userId must not be null");
            Objects.requireNonNull(p.getKey(), "preference.key must not be null");

            UUID userId = p.getUserId().value();
            var key = p.getKey();

            var existing = repo.findByUserIdAndKey(userId, key);

            boolean isInsert = existing.isEmpty();
            UserPreferenceEntity entity = existing.orElseGet(() -> mapper.toEntity(p));

            entity.setUserId(userId);
            entity.setKey(key);
            entity.setValue(p.getValue());
            entity.setUpdatedAt(p.getUpdatedAt() != null ? p.getUpdatedAt() : Instant.now());

            if (entity.getId() == null) {
                entity.setId(UUID.randomUUID());
            }

            UserPreferenceEntity saved = repo.save(entity);

            log.debug(
                    "[UserPreferenceJpaAdapter] - [upsertAll] -> {} preferenceId={} userId={} key={} updatedAt={}",
                    isInsert ? "created" : "updated",
                    saved.getId(),
                    saved.getUserId(),
                    saved.getKey(),
                    saved.getUpdatedAt()
            );

            out.add(mapper.toDomain(saved));
        }

        log.info(
                "[UserPreferenceJpaAdapter] - [upsertAll] -> processedCount={}",
                out.size()
        );

        return out;
    }

    @Override
    public List<UserPreference> findByUserId(UUID userId) {
        Objects.requireNonNull(userId, "userId must not be null");

        log.debug("[UserPreferenceJpaAdapter] - [findByUserId] -> userId={}", userId);

        var list = repo.findByUserId(userId).stream()
                .map(mapper::toDomain)
                .toList();

        log.info(
                "[UserPreferenceJpaAdapter] - [findByUserId] -> userId={} resultSize={}",
                userId,
                list.size()
        );

        return list;
    }
}

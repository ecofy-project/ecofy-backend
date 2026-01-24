package br.com.ecofy.ms_users.config;

import br.com.ecofy.ms_users.adapters.out.persistence.ConnectionJpaAdapter;
import br.com.ecofy.ms_users.adapters.out.persistence.IdempotencyJpaAdapter;
import br.com.ecofy.ms_users.adapters.out.persistence.LinkedAccountJpaAdapter;
import br.com.ecofy.ms_users.adapters.out.persistence.UserPreferenceJpaAdapter;
import br.com.ecofy.ms_users.adapters.out.persistence.UserProfileJpaAdapter;
import br.com.ecofy.ms_users.adapters.out.persistence.mapper.ConnectionMapper;
import br.com.ecofy.ms_users.adapters.out.persistence.mapper.LinkedAccountMapper;
import br.com.ecofy.ms_users.adapters.out.persistence.mapper.PreferenceMapper;
import br.com.ecofy.ms_users.adapters.out.persistence.mapper.UserProfileMapper;
import br.com.ecofy.ms_users.adapters.out.persistence.repository.ConnectionRepository;
import br.com.ecofy.ms_users.adapters.out.persistence.repository.IdempotencyRepository;
import br.com.ecofy.ms_users.adapters.out.persistence.repository.LinkedAccountRepository;
import br.com.ecofy.ms_users.adapters.out.persistence.repository.UserPreferenceRepository;
import br.com.ecofy.ms_users.adapters.out.persistence.repository.UserProfileRepository;
import br.com.ecofy.ms_users.core.port.out.IdempotencyPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Objects;

@Slf4j
@Configuration
public class PersistenceConfig {

    /**
     * Garante que existe um ObjectMapper no contexto (e evita NPE no ConnectionMapper).
     * Se o Spring Boot já tiver um ObjectMapper auto-configurado, este @Bean não será criado.
     */
    // Disponibiliza um ObjectMapper no contexto para ser usado por mappers/serialização na camada de persistência.
    @Bean
    ObjectMapper objectMapper() {
        log.info("[PersistenceConfig] - [objectMapper] -> wiring ObjectMapper");
        return new ObjectMapper().findAndRegisterModules();
    }

    // Disponibiliza o UserProfileMapper (entity <-> domain) no contexto.
    @Bean
    UserProfileMapper userProfileMapper() {
        log.info("[PersistenceConfig] - [userProfileMapper] -> wiring UserProfileMapper");
        return new UserProfileMapper();
    }

    // Disponibiliza o ConnectionMapper (entity <-> domain) no contexto, com suporte a metadata JSON via ObjectMapper.
    @Bean
    ConnectionMapper connectionMapper(ObjectMapper objectMapper) {
        Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        log.info("[PersistenceConfig] - [connectionMapper] -> wiring ConnectionMapper");
        return new ConnectionMapper(objectMapper);
    }

    // Disponibiliza o PreferenceMapper (entity <-> domain) no contexto.
    @Bean
    PreferenceMapper preferenceMapper() {
        log.info("[PersistenceConfig] - [preferenceMapper] -> wiring PreferenceMapper");
        return new PreferenceMapper();
    }

    // Disponibiliza o LinkedAccountMapper (entity <-> domain) no contexto.
    @Bean
    LinkedAccountMapper linkedAccountMapper() {
        log.info("[PersistenceConfig] - [linkedAccountMapper] -> wiring LinkedAccountMapper");
        return new LinkedAccountMapper();
    }

    // Registra o UserProfileJpaAdapter e o expõe também como SaveUserProfilePort e LoadUserProfilePort via aliases de bean.
    @Bean(name = { "userProfileJpaAdapter", "saveUserProfilePort", "loadUserProfilePort" })
    UserProfileJpaAdapter userProfileJpaAdapter(UserProfileRepository repo, UserProfileMapper mapper) {
        Objects.requireNonNull(repo, "userProfileRepository must not be null");
        Objects.requireNonNull(mapper, "userProfileMapper must not be null");

        log.info("[PersistenceConfig] - [userProfileJpaAdapter] -> wiring UserProfileJpaAdapter (Save/Load ports)");
        return new UserProfileJpaAdapter(repo, mapper);
    }

    // Registra o ConnectionJpaAdapter e o expõe também como SaveConnectionPort e LoadConnectionsPort via aliases de bean.
    @Bean(name = { "connectionJpaAdapter", "saveConnectionPort", "loadConnectionsPort" })
    ConnectionJpaAdapter connectionJpaAdapter(ConnectionRepository repo, ConnectionMapper mapper) {
        Objects.requireNonNull(repo, "connectionRepository must not be null");
        Objects.requireNonNull(mapper, "connectionMapper must not be null");

        log.info("[PersistenceConfig] - [connectionJpaAdapter] -> wiring ConnectionJpaAdapter (Save/Load ports)");
        return new ConnectionJpaAdapter(repo, mapper);
    }

    // Registra o UserPreferenceJpaAdapter e o expõe também como SaveUserPreferencePort e LoadUserPreferencesPort via aliases de bean.
    @Bean(name = { "userPreferenceJpaAdapter", "saveUserPreferencePort", "loadUserPreferencesPort" })
    UserPreferenceJpaAdapter userPreferenceJpaAdapter(UserPreferenceRepository repo, PreferenceMapper mapper) {
        Objects.requireNonNull(repo, "userPreferenceRepository must not be null");
        Objects.requireNonNull(mapper, "preferenceMapper must not be null");

        log.info("[PersistenceConfig] - [userPreferenceJpaAdapter] -> wiring UserPreferenceJpaAdapter (Save/Load ports)");
        return new UserPreferenceJpaAdapter(repo, mapper);
    }

    // Registra o LinkedAccountJpaAdapter e o expõe também como SaveLinkedAccountPort e LoadLinkedAccountsPort via aliases de bean.
    @Bean(name = { "linkedAccountJpaAdapter", "saveLinkedAccountPort", "loadLinkedAccountsPort" })
    LinkedAccountJpaAdapter linkedAccountJpaAdapter(LinkedAccountRepository repo, LinkedAccountMapper mapper) {
        Objects.requireNonNull(repo, "linkedAccountRepository must not be null");
        Objects.requireNonNull(mapper, "linkedAccountMapper must not be null");

        log.info("[PersistenceConfig] - [linkedAccountJpaAdapter] -> wiring LinkedAccountJpaAdapter (Save/Load ports)");
        return new LinkedAccountJpaAdapter(repo, mapper);
    }

    // Registra o IdempotencyJpaAdapter responsável pela persistência/consulta de chaves de idempotência.
    @Bean
    IdempotencyJpaAdapter idempotencyJpaAdapter(IdempotencyRepository repo) {
        Objects.requireNonNull(repo, "idempotencyRepository must not be null");
        log.info("[PersistenceConfig] - [idempotencyJpaAdapter] -> wiring IdempotencyJpaAdapter");
        return new IdempotencyJpaAdapter(repo);
    }

    // Expõe o IdempotencyJpaAdapter como IdempotencyPort para a camada de aplicação/domínio.
    @Bean(name = { "idempotencyPort" })
    IdempotencyPort idempotencyPort(IdempotencyJpaAdapter adapter) {
        log.info("[PersistenceConfig] - [idempotencyPort] -> exposing IdempotencyPort");
        return adapter;
    }

}

package br.com.ecofy.ms_users.config;

import br.com.ecofy.ms_users.core.port.out.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PersistenceConfig {

    @Bean
    SaveUserProfilePort saveUserProfilePort(UserProfileRepository repo) {
        return new UserProfileJpaAdapter(repo);
    }

    @Bean
    LoadUserProfilePort loadUserProfilePort(UserProfileRepository repo) {
        return new UserProfileJpaAdapter(repo);
    }

    @Bean
    SaveConnectionPort saveConnectionPort(ConnectionRepository repo) {
        return new ConnectionJpaAdapter(repo);
    }

    @Bean
    LoadConnectionsPort loadConnectionsPort(ConnectionRepository repo) {
        return new ConnectionJpaAdapter(repo);
    }

    @Bean
    SaveUserPreferencePort saveUserPreferencePort(UserPreferenceRepository repo) {
        return new UserPreferenceJpaAdapter(repo);
    }

    @Bean
    LoadUserPreferencesPort loadUserPreferencesPort(UserPreferenceRepository repo) {
        return new UserPreferenceJpaAdapter(repo);
    }

    @Bean
    SaveLinkedAccountPort saveLinkedAccountPort(LinkedAccountRepository repo) {
        return new LinkedAccountJpaAdapter(repo);
    }

    @Bean
    LoadLinkedAccountsPort loadLinkedAccountsPort(LinkedAccountRepository repo) {
        return new LinkedAccountJpaAdapter(repo);
    }

    @Bean
    IdempotencyPort idempotencyPort(IdempotencyRepository repo) {
        return new IdempotencyJpaAdapter(repo);
    }
}
package br.com.ecofy.ms_users.adapters.out.messaging.mapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Mapper de eventos outbound (ms-users -> Kafka).
 *
 * Observação:
 * - Esta classe fica como “placeholder” para manter o padrão de arquitetura.
 * - Quando você adicionar eventos (ex.: user.profile.created, user.preferences.updated),
 *   crie métodos explícitos do tipo `toXEvent(...)` e mantenha logs no padrão:
 *   [EventMapper] - [toXEvent] -> ...
 */
@Slf4j
@Component
public class EventMapper {

    // Exemplo futuro (remova quando implementar de verdade):
    //
    // public UserProfileCreatedEvent toUserProfileCreatedEvent(UserProfile profile) {
    //     Objects.requireNonNull(profile, "profile must not be null");
    //     log.debug("[EventMapper] - [toUserProfileCreatedEvent] -> userId={} profileId={}",
    //             profile.getUserId().value(), profile.getId().value());
    //     return UserProfileCreatedEvent.from(profile);
    // }

}

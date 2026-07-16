package br.com.ecofy.ms_notification.core.port.out;

import br.com.ecofy.ms_notification.core.application.result.NotificationResult;

import java.util.List;
import java.util.UUID;

/**
 * Correção Dia 7 (item #5): antes {@code NotificationQueryService} (core) importava diretamente
 * o adapter concreto {@code NotificationMongoAdapter}. Agora depende deste port de saída.
 */
public interface ListNotificationsPort {

    List<NotificationResult> listByUser(UUID userId, int limit);
}

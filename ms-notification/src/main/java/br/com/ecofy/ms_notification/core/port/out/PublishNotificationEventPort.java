package br.com.ecofy.ms_notification.core.port.out;

import br.com.ecofy.ms_notification.core.domain.Notification;

/**
 * Correção Dia 7 (item #4): antes este port (core) importava o DTO de adapter
 * {@code adapters.out.messaging.dto.NotificationSentEvent}, invertendo a direção de dependência
 * hexagonal. Agora recebe o agregado de domínio {@link Notification}; o adapter Kafka é quem
 * converte para o DTO externo antes de publicar.
 */
public interface PublishNotificationEventPort {
    void publish(Notification notification);
}

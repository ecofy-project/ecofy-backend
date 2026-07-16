package br.com.ecofy.ms_notification.core.port.out;

import br.com.ecofy.ms_notification.core.domain.NotificationTemplate;

/**
 * Correção Dia 7 (item #6): a persistência de templates passa a ser exposta por um port de saída,
 * para que casos de uso/controllers não dependam do adapter Mongo concreto.
 */
public interface SaveNotificationTemplatePort {

    NotificationTemplate save(NotificationTemplate template);
}

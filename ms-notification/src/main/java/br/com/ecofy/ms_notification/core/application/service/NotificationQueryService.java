package br.com.ecofy.ms_notification.core.application.service;

import br.com.ecofy.ms_notification.core.application.result.NotificationResult;
import br.com.ecofy.ms_notification.core.port.in.ListNotificationsUseCase;
import br.com.ecofy.ms_notification.core.port.out.ListNotificationsPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
public class NotificationQueryService implements ListNotificationsUseCase {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    // Correção Dia 7 (item #5): depende do port de saída, não do adapter Mongo concreto.
    private final ListNotificationsPort listNotificationsPort;

    public NotificationQueryService(ListNotificationsPort listNotificationsPort) {
        this.listNotificationsPort = Objects.requireNonNull(listNotificationsPort, "listNotificationsPort must not be null");
    }

    // Lista notificações recentes de um usuário, aplicando normalização de limite (default/máximo) e delegando a consulta ao adapter Mongo.
    @Override
    public List<NotificationResult> listByUser(UUID userId, int limit) {
        Objects.requireNonNull(userId, "userId must not be null");

        int safeLimit = clamp(limit, DEFAULT_LIMIT, MAX_LIMIT);

        log.debug(
                "[NotificationQueryService] - [listByUser] -> listing notifications userId={} limit={}",
                userId,
                safeLimit
        );

        return listNotificationsPort.listByUser(userId, safeLimit);
    }

    // Garante um limite seguro para paginação: aplica default quando inválido e impõe teto máximo para proteger o sistema.
    private static int clamp(int value, int defaultValue, int max) {
        if (value < 1) return defaultValue;
        return Math.min(value, max);
    }

}

package br.com.ecofy.ms_notification.adapters.out.persistence.mongo;

import br.com.ecofy.ms_notification.adapters.out.persistence.mapper.AttemptMapper;
import br.com.ecofy.ms_notification.adapters.out.persistence.repository.DeliveryAttemptMongoRepository;
import br.com.ecofy.ms_notification.core.domain.DeliveryAttempt;
import br.com.ecofy.ms_notification.core.domain.valueobject.NotificationId;
import br.com.ecofy.ms_notification.core.port.out.SaveDeliveryAttemptPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
public class DeliveryAttemptMongoAdapter implements SaveDeliveryAttemptPort {

    private final DeliveryAttemptMongoRepository repo;
    private final AttemptMapper mapper;

    public DeliveryAttemptMongoAdapter(DeliveryAttemptMongoRepository repo, AttemptMapper mapper) {
        this.repo = Objects.requireNonNull(repo, "repo must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    // Persiste um DeliveryAttempt no Mongo (via mapper), retornando o domínio reconstituído e registrando logs de sucesso/erro.
    @Override
    public DeliveryAttempt save(DeliveryAttempt attempt) {
        if (attempt == null) throw new IllegalArgumentException("attempt must not be null");

        UUID notificationId = attempt.getNotificationId() != null ? attempt.getNotificationId().value() : null;
        Integer attemptNumber = attempt.getAttemptNumber();
        var status = attempt.getStatus();

        try {
            var saved = repo.save(mapper.toDoc(attempt));
            var domain = mapper.toDomain(saved);

            log.debug(
                    "[DeliveryAttemptMongoAdapter] - [save] -> saved attemptId={} notificationId={} attemptNumber={} status={}",
                    domain.getId(),
                    domain.getNotificationId() != null ? domain.getNotificationId().value() : notificationId,
                    domain.getAttemptNumber(),
                    domain.getStatus()
            );

            return domain;
        } catch (Exception ex) {
            log.error(
                    "[DeliveryAttemptMongoAdapter] - [save] -> Falha ao salvar tentativa notificationId={} attemptNumber={} status={}",
                    notificationId,
                    attemptNumber,
                    status,
                    ex
            );
            throw ex;
        }
    }

    // Carrega todas as tentativas de entrega de uma notificação (ordenadas por attemptNumber), mapeando documentos para domínio.
    @Override
    public List<DeliveryAttempt> loadByNotificationId(NotificationId notificationId) {
        Objects.requireNonNull(notificationId, "notificationId must not be null");

        UUID id = Objects.requireNonNull(notificationId.value(), "notificationId.value must not be null");

        try {
            var list = repo.findByNotificationIdOrderByAttemptNumberAsc(id).stream()
                    .map(mapper::toDomain)
                    .toList();

            log.debug(
                    "[DeliveryAttemptMongoAdapter] - [loadByNotificationId] -> loaded attempts notificationId={} count={}",
                    id,
                    list.size()
            );

            return list;
        } catch (Exception ex) {
            log.error(
                    "[DeliveryAttemptMongoAdapter] - [loadByNotificationId] -> Falha ao carregar tentativas notificationId={}",
                    id,
                    ex
            );
            throw ex;
        }
    }

}

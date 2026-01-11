package br.com.ecofy.ms_notification.adapters.out.persistence.mongo;

import br.com.ecofy.ms_notification.adapters.out.persistence.mapper.NotificationMapper;
import br.com.ecofy.ms_notification.adapters.out.persistence.repository.NotificationMongoRepository;
import br.com.ecofy.ms_notification.core.application.result.NotificationResult;
import br.com.ecofy.ms_notification.core.domain.Notification;
import br.com.ecofy.ms_notification.core.domain.valueobject.NotificationId;
import br.com.ecofy.ms_notification.core.port.out.SaveNotificationPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class NotificationMongoAdapter implements SaveNotificationPort {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    private final NotificationMongoRepository repo;
    private final NotificationMapper mapper;

    public NotificationMongoAdapter(NotificationMongoRepository repo, NotificationMapper mapper) {
        this.repo = Objects.requireNonNull(repo, "repo must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public Notification save(Notification notification) {
        if (notification == null) throw new IllegalArgumentException("notification must not be null");

        UUID notificationId = notification.getId() != null ? notification.getId().value() : null;
        UUID userId = notification.getUserId() != null ? notification.getUserId().value() : null;

        try {
            var saved = repo.save(mapper.toDoc(notification));
            var domain = mapper.toDomain(saved);

            log.debug(
                    "[NotificationMongoAdapter] - [save] -> saved notificationId={} userId={} status={} attemptCount={}",
                    domain.getId() != null ? domain.getId().value() : notificationId,
                    domain.getUserId() != null ? domain.getUserId().value() : userId,
                    domain.getStatus(),
                    domain.getAttemptCount()
            );

            return domain;
        } catch (Exception ex) {
            log.error(
                    "[NotificationMongoAdapter] - [save] -> failed to save notificationId={} userId={}",
                    notificationId,
                    userId,
                    ex
            );
            throw ex;
        }
    }

    @Override
    public Optional<Notification> loadById(NotificationId id) {
        Objects.requireNonNull(id, "id must not be null");
        UUID uuid = Objects.requireNonNull(id.value(), "id.value must not be null");

        try {
            var opt = repo.findById(uuid).map(mapper::toDomain);

            log.debug(
                    "[NotificationMongoAdapter] - [loadById] -> loaded notificationId={} found={}",
                    uuid,
                    opt.isPresent()
            );

            return opt;
        } catch (Exception ex) {
            log.error(
                    "[NotificationMongoAdapter] - [loadById] -> failed to load notificationId={}",
                    uuid,
                    ex
            );
            throw ex;
        }
    }

    public List<NotificationResult> listByUser(UUID userId, int limit) {
        Objects.requireNonNull(userId, "userId must not be null");

        int safeLimit = clamp(limit, DEFAULT_LIMIT, MAX_LIMIT);

        try {
            var list = repo.findByUserIdOrderByCreatedAtDesc(userId).stream()
                    .limit(safeLimit)
                    .map(NotificationMongoAdapter::toResult)
                    .toList();

            log.debug(
                    "[NotificationMongoAdapter] - [listByUser] -> listed notifications userId={} limit={} returned={}",
                    userId,
                    safeLimit,
                    list.size()
            );

            return list;
        } catch (Exception ex) {
            log.error(
                    "[NotificationMongoAdapter] - [listByUser] -> failed to list notifications userId={} limit={}",
                    userId,
                    safeLimit,
                    ex
            );
            throw ex;
        }
    }

    private static NotificationResult toResult(br.com.ecofy.ms_notification.adapters.out.persistence.document.NotificationDocument d) {
        return new NotificationResult(
                d.getId(),
                d.getUserId(),
                d.getEventType(),
                d.getChannel(),
                d.getDestination(),
                d.getSubject(),
                d.getBody(),
                d.getStatus(),
                d.getAttemptCount(),
                d.getPayload(),
                d.getCreatedAt(),
                d.getUpdatedAt()
        );
    }

    private static int clamp(Integer value, int defaultValue, int max) {
        if (value == null || value < 1) return defaultValue;
        return Math.min(value, max);
    }
}

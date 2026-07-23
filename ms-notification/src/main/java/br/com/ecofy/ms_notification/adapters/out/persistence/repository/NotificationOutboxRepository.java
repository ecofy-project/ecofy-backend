package br.com.ecofy.ms_notification.adapters.out.persistence.repository;

import br.com.ecofy.ms_notification.adapters.out.persistence.document.NotificationOutboxDocument;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificationOutboxRepository extends MongoRepository<NotificationOutboxDocument, UUID> {

    List<NotificationOutboxDocument> findByStatusOrderByCreatedAtAsc(String status, Pageable pageable);

    List<NotificationOutboxDocument> findByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            String status, Instant now, Pageable pageable);

    long countByStatus(String status);
}

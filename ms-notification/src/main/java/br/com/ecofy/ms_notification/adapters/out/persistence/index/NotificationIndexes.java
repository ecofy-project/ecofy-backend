package br.com.ecofy.ms_notification.adapters.out.persistence.index;

import br.com.ecofy.ms_notification.adapters.out.persistence.document.NotificationDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
public class NotificationIndexes implements ApplicationRunner {

    private static final String IDX_USER_CREATED_AT = "idx_notifications_user_createdAt";
    private static final String UX_IDEMPOTENCY_KEY = "ux_notifications_idempotencyKey";

    private static final String FIELD_USER_ID = "userId";
    private static final String FIELD_CREATED_AT = "createdAt";
    private static final String FIELD_IDEMPOTENCY_KEY = "idempotencyKey";

    private final MongoTemplate mongoTemplate;

    public NotificationIndexes(MongoTemplate mongoTemplate) {
        this.mongoTemplate = Objects.requireNonNull(mongoTemplate, "mongoTemplate must not be null");
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureIndexes();
    }

    void ensureIndexes() {
        try {
            String collection = mongoTemplate.getCollectionName(NotificationDocument.class);

            String idx1 = mongoTemplate.indexOps(NotificationDocument.class).createIndex(userCreatedAtIndex());
            String idx2 = mongoTemplate.indexOps(NotificationDocument.class).createIndex(idempotencyKeyUniqueIndex());

            log.info(
                    "[NotificationIndexes] - [ensureIndexes] -> ensured indexes collection={} idx1={} idx2={}",
                    collection,
                    idx1,
                    idx2
            );
        } catch (Exception ex) {
            log.error(
                    "[NotificationIndexes] - [ensureIndexes] -> failed to ensure indexes for NotificationDocument",
                    ex
            );
            throw ex;
        }
    }

    private static Index userCreatedAtIndex() {
        return new Index()
                .on(FIELD_USER_ID, Sort.Direction.ASC)
                .on(FIELD_CREATED_AT, Sort.Direction.DESC)
                .named(IDX_USER_CREATED_AT);
    }

    private static Index idempotencyKeyUniqueIndex() {
        return new Index()
                .on(FIELD_IDEMPOTENCY_KEY, Sort.Direction.ASC)
                .unique()
                .sparse()
                .named(UX_IDEMPOTENCY_KEY);
    }
}

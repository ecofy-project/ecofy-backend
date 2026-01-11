package br.com.ecofy.ms_notification.adapters.out.persistence.index;

import br.com.ecofy.ms_notification.adapters.out.persistence.document.IdempotencyKeyDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class IdempotencyIndexes implements ApplicationRunner {

    private static final String TTL_INDEX_NAME = "ttl_idempotency_expiresAt";
    private static final String FIELD_EXPIRES_AT = "expiresAt";

    private final MongoTemplate mongoTemplate;

    public IdempotencyIndexes(MongoTemplate mongoTemplate) {
        this.mongoTemplate = Objects.requireNonNull(mongoTemplate, "mongoTemplate must not be null");
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureIndexes();
    }

    void ensureIndexes() {
        try {
            String indexName = mongoTemplate.indexOps(IdempotencyKeyDocument.class)
                    .createIndex(ttlIndexDefinition());

            log.info(
                    "[IdempotencyIndexes] - [ensureIndexes] -> ensured TTL index name={} createdOrExisting={}",
                    TTL_INDEX_NAME,
                    indexName
            );
        } catch (Exception ex) {
            log.error(
                    "[IdempotencyIndexes] - [ensureIndexes] -> failed to ensure TTL index name={}",
                    TTL_INDEX_NAME,
                    ex
            );
            throw ex;
        }
    }

    private static Index ttlIndexDefinition() {
        return new Index()
                .on(FIELD_EXPIRES_AT, Sort.Direction.ASC)
                .expire(0, TimeUnit.SECONDS)
                .named(TTL_INDEX_NAME);
    }
}

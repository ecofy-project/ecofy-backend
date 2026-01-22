package br.com.ecofy.ms_notification.adapters.out.persistence.index;

import br.com.ecofy.ms_notification.adapters.out.persistence.document.IdempotencyKeyDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;

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
        IndexOperations ops = mongoTemplate.indexOps(IdempotencyKeyDocument.class);

        try {
            // createIndex é idempotente: se já existir com a mesma definição, apenas retorna o nome.
            String createdOrExisting = ops.createIndex(ttlIndexDefinition());

            log.info(
                    "[IdempotencyIndexes] - [ensureIndexes] -> ensured TTL index indexName={} createdOrExisting={}",
                    TTL_INDEX_NAME,
                    createdOrExisting
            );

        } catch (Exception ex) {
            log.error(
                    "[IdempotencyIndexes] - [ensureIndexes] -> failed to ensure TTL index indexName={}",
                    TTL_INDEX_NAME,
                    ex
            );
            throw ex;
        }
    }

    // TTL index:
    // - expireAfter = 0: expira exatamente no timestamp do campo expiresAt.
    // - Requer que expiresAt seja Date/Instant.
    private static Index ttlIndexDefinition() {
        return new Index()
                .on(FIELD_EXPIRES_AT, Sort.Direction.ASC)
                .expire(Duration.ZERO) // Spring Data MongoDB: preferível a TimeUnit p/ APIs mais novas
                .named(TTL_INDEX_NAME);
    }

}

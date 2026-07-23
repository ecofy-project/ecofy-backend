package br.com.ecofy.ms_notification.adapters.out.persistence.mongo;

import br.com.ecofy.ms_notification.adapters.out.persistence.document.IdempotencyKeyDocument;
import br.com.ecofy.ms_notification.adapters.out.persistence.repository.IdempotencyMongoRepository;
import br.com.ecofy.ms_notification.config.NotificationProperties;
import br.com.ecofy.ms_notification.core.domain.valueobject.IdempotencyKey;
import br.com.ecofy.ms_notification.core.port.out.IdempotencyPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Objects;

@Slf4j
@Component
public class IdempotencyMongoAdapter implements IdempotencyPort {

    private final IdempotencyMongoRepository repo;
    private final NotificationProperties props;

    public IdempotencyMongoAdapter(IdempotencyMongoRepository repo, NotificationProperties props) {
        this.repo = Objects.requireNonNull(repo, "repo must not be null");
        this.props = Objects.requireNonNull(props, "props must not be null");
        Objects.requireNonNull(props.getIdempotency(), "props.idempotency must not be null");
        Objects.requireNonNull(props.getIdempotency().getTtl(), "props.idempotency.ttl must not be null");
    }

    // Tenta adquirir uma chave de idempotência via insert (garantido por unique index), calculando expiresAt com TTL e retornando true/false conforme sucesso/duplicidade.
    @Override
    public boolean tryAcquire(IdempotencyKey key) {
        if (key == null) throw new IllegalArgumentException("key must not be null");
        if (key.value() == null || key.value().isBlank()) throw new IllegalArgumentException("key.value must not be blank");

        var now = Instant.now();
        var ttl = props.getIdempotency().getTtl();
        var expiresAt = now.plus(ttl);

        var doc = IdempotencyKeyDocument.builder()
                .key(key.value())
                .createdAt(now)
                .expiresAt(expiresAt)
                .build();

        try {
            repo.insert(doc);

            log.debug(
                    "[IdempotencyMongoAdapter] - [tryAcquire] -> Chave de idempotência adquirida key={} expiresAt={}",
                    key.value(),
                    expiresAt
            );

            return true;
        } catch (DuplicateKeyException ex) {
            log.warn(
                    "[IdempotencyMongoAdapter] - [tryAcquire] -> Chave de idempotência já utilizada key={}",
                    key.value()
            );
            return false;
        } catch (Exception ex) {
            log.error(
                    "[IdempotencyMongoAdapter] - [tryAcquire] -> Falha ao adquirir chave de idempotência key={}",
                    key.value(),
                    ex
            );
            throw ex;
        }
    }

}

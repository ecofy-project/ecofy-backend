package br.com.ecofy.ms_notification.adapters.out.persistence.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "idempotency_keys")
public class IdempotencyKeyDocument {

    @Id
    private String key; // unique

    private Instant createdAt;

    @Indexed(name = "ttl_idempotency_expiresAt", expireAfter = "0s")
    private Instant expiresAt;

}
package br.com.ecofy.ms_ingestion.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Garante que os tópicos Kafka bindam no prefixo UNIFICADO ecofy.ingestion.kafka.topics.*
 * (antes havia divergência com ecofy.ingestion.topics, ignorada pelo KafkaConfig).
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:msingestion_kafka;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.kafka.listener.auto-startup=false",
        "spring.kafka.admin.auto-create=false",
        "ecofy.ingestion.security.permit-all=true"
})
class KafkaTopicsBindingTest {

    @Autowired
    private KafkaConfig.IngestionTopics topics;

    @Test
    void topics_shouldBindFromUnifiedPrefix() {
        assertEquals("eco.ingestion.transaction.imported", topics.getTransactionImported());
        assertEquals("eco.ingestion.import-job.status-changed", topics.getImportJobStatusChanged());
        // Tópico publicado para ms-categorization — deve estar estável/preservado.
        assertEquals("eco.categorization.request", topics.getCategorizationRequest());
    }
}

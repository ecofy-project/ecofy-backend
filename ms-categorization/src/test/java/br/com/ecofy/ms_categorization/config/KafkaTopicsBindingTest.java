package br.com.ecofy.ms_categorization.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Garante que os tópicos e o consumer group bindam corretamente após a unificação:
 * fonte única = ecofy.categorization.topics.* + spring.kafka.consumer.group-id central.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:mscategorization_kafka;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.kafka.listener.auto-startup=false",
        "spring.kafka.admin.auto-create=false",
        "ecofy.categorization.security.permit-all=true"
})
class KafkaTopicsBindingTest {

    @Autowired
    private CategorizationProperties props;

    @Autowired
    private Environment env;

    @Test
    void topics_bindFromUnifiedSource() {
        assertEquals("eco.categorization.request", props.getTopics().getCategorizationRequest());
        assertEquals("eco.transaction.categorized", props.getTopics().getTransactionCategorized());
        assertEquals("eco.categorization.applied", props.getTopics().getCategorizationApplied());
    }

    @Test
    void consumerGroup_usesCentralConfig() {
        assertEquals("ms-categorization-v2", env.getProperty("spring.kafka.consumer.group-id"));
    }
}

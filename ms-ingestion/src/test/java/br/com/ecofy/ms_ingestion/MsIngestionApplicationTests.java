package br.com.ecofy.ms_ingestion;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Teste de contexto. Roda com H2 em memória e sem broker Kafka (listeners e
 * auto-create de tópicos desligados), subindo sem depender de Postgres/Kafka externos.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:msingestion;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.kafka.listener.auto-startup=false",
        "spring.kafka.admin.auto-create=false",
        "ecofy.ingestion.security.permit-all=true"
})
class MsIngestionApplicationTests {

	@Test
	void contextLoads() {
	}

}

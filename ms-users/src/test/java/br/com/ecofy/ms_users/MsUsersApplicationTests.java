package br.com.ecofy.ms_users;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Teste de contexto. Roda com banco H2 em memória (perfil "test") e sem broker Kafka
 * (listeners com auto-startup=false), de modo que sobe sem depender de Postgres/Kafka externos.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:msusers;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.kafka.listener.auto-startup=false",
        "ecofy.users.security.permit-all=true"
})
@ActiveProfiles("test")
class MsUsersApplicationTests {

	@Test
	void contextLoads() {
	}

}

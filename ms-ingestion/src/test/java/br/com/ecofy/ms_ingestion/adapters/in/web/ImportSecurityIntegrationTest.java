package br.com.ecofy.ms_ingestion.adapters.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Segurança de /api/import/** com permit-all=false (comportamento de produção):
 * sem JWT -> 401; com JWT válido -> passa pela segurança (não 401).
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:msingestion_sec;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.kafka.listener.auto-startup=false",
        "spring.kafka.admin.auto-create=false",
        "ecofy.ingestion.security.permit-all=false"
})
@AutoConfigureMockMvc
class ImportSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getJobStatus_withoutJwt_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/import/jobs/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void uploadFile_withoutJwt_shouldReturn401() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "tx.csv", MediaType.TEXT_PLAIN_VALUE, "date;description;amount\n2026-01-15;Coffee;12.50".getBytes());

        mockMvc.perform(multipart("/api/import/file").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getJobStatus_withValidJwt_shouldPassSecurity_thenNotFound() throws Exception {
        // Com JWT válido a segurança é satisfeita; o job inexistente resulta em 404 (não 401).
        mockMvc.perform(get("/api/import/jobs/{id}", UUID.randomUUID()).with(jwt()))
                .andExpect(status().isNotFound());
    }
}

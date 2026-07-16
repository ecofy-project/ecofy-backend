package br.com.ecofy.ms_categorization.adapters.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Segurança de /api/categorization/** com permit-all=false (produção):
 * sem JWT -> 401; com JWT válido -> acessa (não 401).
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:mscategorization_sec;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.kafka.listener.auto-startup=false",
        "spring.kafka.admin.auto-create=false",
        "ecofy.categorization.security.permit-all=false"
})
@AutoConfigureMockMvc
class CategorizationSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listCategories_withoutJwt_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/categorization/v1/categories"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createCategory_withoutJwt_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/categorization/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Food\",\"color\":\"#fff\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listCategories_withValidJwt_shouldReturn200() throws Exception {
        mockMvc.perform(get("/api/categorization/v1/categories").with(jwt()))
                .andExpect(status().isOk());
    }
}

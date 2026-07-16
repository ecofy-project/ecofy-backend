package br.com.ecofy.ms_notification.adapters.in.web;

import br.com.ecofy.ms_notification.adapters.in.web.advice.RestExceptionHandler;
import br.com.ecofy.ms_notification.core.application.result.TemplatePreviewResult;
import br.com.ecofy.ms_notification.core.domain.NotificationTemplate;
import br.com.ecofy.ms_notification.core.domain.enums.DomainEventType;
import br.com.ecofy.ms_notification.core.domain.enums.NotificationChannel;
import br.com.ecofy.ms_notification.core.domain.enums.TemplateEngine;
import br.com.ecofy.ms_notification.core.domain.valueobject.TemplateId;
import br.com.ecofy.ms_notification.core.port.in.CreateTemplateUseCase;
import br.com.ecofy.ms_notification.core.port.in.GetTemplateUseCase;
import br.com.ecofy.ms_notification.core.port.in.PreviewTemplateUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TemplateController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(RestExceptionHandler.class)
class TemplateControllerWebTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CreateTemplateUseCase createUseCase;
    @MockitoBean
    private GetTemplateUseCase getUseCase;
    @MockitoBean
    private PreviewTemplateUseCase previewUseCase;

    private NotificationTemplate template(UUID id) {
        var now = Instant.now();
        return NotificationTemplate.builder()
                .id(new TemplateId(id))
                .ownerUserId(null)
                .eventType(DomainEventType.BUDGET_ALERT)
                .channel(NotificationChannel.EMAIL)
                .engine(TemplateEngine.SIMPLE)
                .subjectTemplate("Assunto")
                .bodyTemplate("Corpo")
                .active(true)
                .createdAt(now).updatedAt(now)
                .build();
    }

    @Test
    void create_shouldReturn201ViaUseCase() throws Exception {
        UUID id = UUID.randomUUID();
        when(createUseCase.create(any())).thenReturn(template(id));

        String body = """
                {"eventType":"BUDGET_ALERT","channel":"EMAIL","engine":"SIMPLE","subjectTemplate":"Assunto","bodyTemplate":"Corpo","active":true}
                """;

        mvc.perform(post("/api/notification/v1/templates")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.channel").value("EMAIL"));
    }

    @Test
    void get_shouldReturn200_whenFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(getUseCase.findById(any())).thenReturn(Optional.of(template(id)));

        mvc.perform(get("/api/notification/v1/templates/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void get_shouldReturn404_whenMissing() throws Exception {
        when(getUseCase.findById(any())).thenReturn(Optional.empty());

        mvc.perform(get("/api/notification/v1/templates/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void preview_shouldReturn200() throws Exception {
        UUID userId = UUID.randomUUID();
        when(previewUseCase.preview(any())).thenReturn(new TemplatePreviewResult("Assunto", "Corpo"));

        String body = """
                {"userId":"%s","eventType":"BUDGET_ALERT","channel":"EMAIL","payload":{}}
                """.formatted(userId);

        mvc.perform(post("/api/notification/v1/templates/preview")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("Assunto"))
                .andExpect(jsonPath("$.body").value("Corpo"));
    }
}

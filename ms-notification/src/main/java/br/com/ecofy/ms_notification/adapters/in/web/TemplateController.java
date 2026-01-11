package br.com.ecofy.ms_notification.adapters.in.web;

import br.com.ecofy.ms_notification.adapters.in.web.dto.SendNotificationRequest;
import br.com.ecofy.ms_notification.adapters.in.web.dto.TemplatePreviewResponse;
import br.com.ecofy.ms_notification.adapters.in.web.dto.TemplateRequest;
import br.com.ecofy.ms_notification.adapters.in.web.dto.TemplateResponse;
import br.com.ecofy.ms_notification.adapters.out.persistence.mongo.NotificationTemplateMongoAdapter;
import br.com.ecofy.ms_notification.core.application.command.PreviewTemplateCommand;
import br.com.ecofy.ms_notification.core.domain.NotificationTemplate;
import br.com.ecofy.ms_notification.core.domain.valueobject.TemplateId;
import br.com.ecofy.ms_notification.core.domain.valueobject.UserId;
import br.com.ecofy.ms_notification.core.port.in.PreviewTemplateUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@Tag(name = "Notification Templates", description = "Cadastro e preview de templates de notificação")
@RequestMapping(path = "/api/notification/v1/templates", produces = MediaType.APPLICATION_JSON_VALUE)
public class TemplateController {

    private final NotificationTemplateMongoAdapter templateAdapter;
    private final PreviewTemplateUseCase previewUseCase;

    @Operation(
            summary = "Cria um template",
            description = """
                    Cria um template para um eventType/canal/engine.
                    ownerUserId pode ser nulo (template global) ou informado (template do usuário).
                    
                    Resposta:
                    - 201 (Created) com `Location` apontando para o recurso criado.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Template criado com sucesso",
                    content = @Content(schema = @Schema(implementation = TemplateResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Payload inválido / regras de domínio violadas"),
            @ApiResponse(responseCode = "401", description = "Não autenticado (JWT ausente/inválido)"),
            @ApiResponse(responseCode = "500", description = "Erro interno ao criar template")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TemplateResponse> create(@Valid @RequestBody TemplateRequest req) {
        log.info(
                "[TemplateController] - [create] -> ownerUserId={} eventType={} channel={} engine={} active={}",
                req.ownerUserId(), req.eventType(), req.channel(), req.engine(), req.active()
        );

        var now = Instant.now();

        var template = NotificationTemplate.builder()
                .id(TemplateId.newId())
                .ownerUserId(req.ownerUserId() == null ? null : new UserId(req.ownerUserId()))
                .eventType(req.eventType())
                .channel(req.channel())
                .engine(req.engine())
                .subjectTemplate(req.subjectTemplate())
                .bodyTemplate(req.bodyTemplate())
                .active(req.active())
                .createdAt(now)
                .updatedAt(now)
                .build()
                .validate();

        var saved = templateAdapter.save(template);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId().value())
                .toUri();

        return ResponseEntity.created(location).body(toResponse(saved));
    }

    @Operation(
            summary = "Busca template por ID",
            description = "Retorna um template específico pelo ID."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Template retornado com sucesso",
                    content = @Content(schema = @Schema(implementation = TemplateResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autenticado (JWT ausente/inválido)"),
            @ApiResponse(responseCode = "404", description = "Template não encontrado"),
            @ApiResponse(responseCode = "500", description = "Erro interno ao buscar template")
    })
    @GetMapping("/{id}")
    public ResponseEntity<TemplateResponse> get(@PathVariable("id") @NotNull UUID id) {
        log.debug("[TemplateController] - [get] -> id={}", id);

        return templateAdapter.loadById(new TemplateId(id))
                .map(t -> ResponseEntity.ok(toResponse(t)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Preview de template",
            description = """
                    Gera preview (subject/body) para um eventType e canal usando o payload.
                    Útil para validar templates antes de enviar notificações.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Preview gerado com sucesso",
                    content = @Content(schema = @Schema(implementation = TemplatePreviewResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Payload inválido / regras de domínio violadas"),
            @ApiResponse(responseCode = "401", description = "Não autenticado (JWT ausente/inválido)"),
            @ApiResponse(responseCode = "500", description = "Erro interno ao gerar preview")
    })
    @PostMapping(path = "/preview", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TemplatePreviewResponse> preview(@Valid @RequestBody SendNotificationRequest req) {
        log.debug(
                "[TemplateController] - [preview] -> userId={} eventType={} channel={} hasPayload={}",
                req.userId(), req.eventType(), req.channel(),
                req.payload() != null && !req.payload().isEmpty()
        );

        var result = previewUseCase.preview(new PreviewTemplateCommand(
                req.userId(),
                req.eventType(),
                req.channel(),
                req.payload()
        ));

        return ResponseEntity.ok(new TemplatePreviewResponse(result.subject(), result.body()));
    }

    private static TemplateResponse toResponse(NotificationTemplate t) {
        return new TemplateResponse(
                t.getId().value(),
                t.getOwnerUserId() == null ? null : t.getOwnerUserId().value(),
                t.getEventType(),
                t.getChannel(),
                t.getEngine(),
                t.getSubjectTemplate(),
                t.getBodyTemplate(),
                t.isActive(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }
}

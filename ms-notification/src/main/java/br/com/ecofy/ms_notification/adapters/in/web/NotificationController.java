package br.com.ecofy.ms_notification.adapters.in.web;

import br.com.ecofy.ms_notification.adapters.in.web.dto.NotificationResponse;
import br.com.ecofy.ms_notification.adapters.in.web.dto.ResendRequest;
import br.com.ecofy.ms_notification.adapters.in.web.dto.SendNotificationRequest;
import br.com.ecofy.ms_notification.core.application.command.ResendNotificationCommand;
import br.com.ecofy.ms_notification.core.application.command.SendNotificationCommand;
import br.com.ecofy.ms_notification.core.application.result.NotificationResult;
import br.com.ecofy.ms_notification.core.port.in.ListNotificationsUseCase;
import br.com.ecofy.ms_notification.core.port.in.ResendNotificationUseCase;
import br.com.ecofy.ms_notification.core.port.in.SendNotificationUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@Tag(name = "Notification", description = "Envio, reenvio e consulta de notificações")
@RequestMapping(path = "/api/notification/v1/notifications", produces = MediaType.APPLICATION_JSON_VALUE)
public class NotificationController {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    private final SendNotificationUseCase sendUseCase;
    private final ResendNotificationUseCase resendUseCase;
    private final ListNotificationsUseCase listUseCase;

    @Operation(
            summary = "Envia uma notificação",
            description = """
                    Envia uma notificação sob demanda.
                    
                    Idempotência:
                    - Envie o header `Idempotency-Key` para evitar duplicidade em retries.
                    
                    Resposta:
                    - 201 (Created) com `Location` apontando para o recurso recém-criado.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Notificação criada com sucesso",
                    content = @Content(schema = @Schema(implementation = NotificationResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Payload inválido / regras de domínio violadas"),
            @ApiResponse(responseCode = "401", description = "Não autenticado (JWT ausente/inválido)"),
            @ApiResponse(responseCode = "409", description = "Conflito de idempotência"),
            @ApiResponse(responseCode = "500", description = "Erro interno ao enviar notificação")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<NotificationResponse> send(
            @RequestHeader(name = IDEMPOTENCY_KEY_HEADER, required = false)
            @Size(min = 8, max = 200, message = "Idempotency-Key must be between 8 and 200 chars")
            String idempotencyKey,
            @Valid @RequestBody SendNotificationRequest request
    ) {
        String effectiveIdem = (idempotencyKey != null && !idempotencyKey.isBlank())
                ? idempotencyKey
                : request.idempotencyKey();

        log.info(
                "[NotificationController] - [send] -> userId={} eventType={} channel={} hasDestinationOverride={} hasPayload={} hasIdempotencyKey={}",
                request.userId(),
                request.eventType(),
                request.channel(),
                request.destinationOverride() != null,
                request.payload() != null && !request.payload().isEmpty(),
                effectiveIdem != null && !effectiveIdem.isBlank()
        );

        var cmd = new SendNotificationCommand(
                request.userId(),
                request.eventType(),
                request.channel(),
                request.destinationOverride(),
                request.payload(),
                effectiveIdem
        );

        NotificationResult result = sendUseCase.send(cmd);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(result.id())
                .toUri();

        return ResponseEntity.created(location).body(toResponse(result));
    }

    @Operation(
            summary = "Reenvia uma notificação",
            description = """
                    Solicita reenvio de uma notificação existente.
                    
                    Idempotência:
                    - Envie o header `Idempotency-Key` para evitar reenvios duplicados em retries.
                    
                    Resposta:
                    - 200 (OK) com os dados da notificação.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Reenvio disparado com sucesso",
                    content = @Content(schema = @Schema(implementation = NotificationResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Payload inválido / regras de domínio violadas"),
            @ApiResponse(responseCode = "401", description = "Não autenticado (JWT ausente/inválido)"),
            @ApiResponse(responseCode = "404", description = "Notificação não encontrada"),
            @ApiResponse(responseCode = "409", description = "Conflito de idempotência"),
            @ApiResponse(responseCode = "500", description = "Erro interno ao reenviar notificação")
    })
    @PostMapping(path = "/resend", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<NotificationResponse> resend(
            @RequestHeader(name = IDEMPOTENCY_KEY_HEADER, required = false)
            @Size(min = 8, max = 200, message = "Idempotency-Key must be between 8 and 200 chars")
            String idempotencyKey,
            @Valid @RequestBody ResendRequest request
    ) {
        String effectiveIdem = (idempotencyKey != null && !idempotencyKey.isBlank())
                ? idempotencyKey
                : request.idempotencyKey();

        log.info(
                "[NotificationController] - [resend] -> notificationId={} hasIdempotencyKey={}",
                request.notificationId(),
                effectiveIdem != null && !effectiveIdem.isBlank()
        );

        var cmd = new ResendNotificationCommand(
                request.notificationId(),
                effectiveIdem
        );

        NotificationResult result = resendUseCase.resend(cmd);

        return ResponseEntity.ok(toResponse(result));
    }

    @Operation(
            summary = "Lista notificações por usuário",
            description = """
                    Retorna notificações do usuário.
                    
                    Observações:
                    - limit padrão: 50
                    - limit máximo: 200
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista retornada com sucesso",
                    content = @Content(schema = @Schema(implementation = NotificationResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Parâmetros inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autenticado (JWT ausente/inválido)"),
            @ApiResponse(responseCode = "500", description = "Erro interno ao listar notificações")
    })
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> list(
            @RequestParam("userId") UUID userId,
            @RequestParam(name = "limit", defaultValue = "" + DEFAULT_LIMIT) Integer limit
    ) {
        int safeLimit = clamp(limit, DEFAULT_LIMIT, MAX_LIMIT);

        log.debug("[NotificationController] - [list] -> userId={} limit={}", userId, safeLimit);

        var list = listUseCase.listByUser(userId, safeLimit).stream()
                .map(NotificationController::toResponse)
                .toList();

        return ResponseEntity.ok(list);
    }

    private static int clamp(Integer value, int defaultValue, int max) {
        if (value == null) return defaultValue;
        if (value < 1) return defaultValue;
        return Math.min(value, max);
    }

    private static NotificationResponse toResponse(NotificationResult r) {
        return new NotificationResponse(
                r.id(), r.userId(), r.eventType(), r.channel(), r.destination(),
                r.subject(), r.body(), r.status(), r.attemptCount(), r.payload(),
                r.createdAt(), r.updatedAt()
        );
    }
}

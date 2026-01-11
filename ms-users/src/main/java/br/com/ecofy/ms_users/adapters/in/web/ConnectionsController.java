package br.com.ecofy.ms_users.adapters.in.web;

import br.com.ecofy.ms_users.adapters.in.web.dto.ConnectionResponse;
import br.com.ecofy.ms_users.adapters.in.web.dto.CreateConnectionRequest;
import br.com.ecofy.ms_users.core.application.command.CreateConnectionCommand;
import br.com.ecofy.ms_users.core.application.result.ConnectionResult;
import br.com.ecofy.ms_users.core.port.in.CreateConnectionUseCase;
import br.com.ecofy.ms_users.core.port.in.ListConnectionsUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(path = "/api/users/v1/connections", produces = MediaType.APPLICATION_JSON_VALUE)
public class ConnectionsController {

    private final CreateConnectionUseCase createConnectionUseCase;
    private final ListConnectionsUseCase listConnectionsUseCase;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ConnectionResponse> create(
            @RequestHeader(name = "Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateConnectionRequest req
    ) {
        var result = createConnectionUseCase.create(new CreateConnectionCommand(
                req.userId(),
                req.type(),
                req.provider(),
                req.metadata(),
                idempotencyKey
        ));
        return ResponseEntity.ok(toResponse(result));
    }

    @GetMapping
    public ResponseEntity<List<ConnectionResponse>> list(@RequestParam UUID userId) {
        var result = listConnectionsUseCase.listByUserId(userId).stream()
                .map(ConnectionsController::toResponse)
                .toList();
        return ResponseEntity.ok(result);
    }

    private static ConnectionResponse toResponse(ConnectionResult r) {
        return new ConnectionResponse(
                r.id(),
                r.userId(),
                r.type(),
                r.provider(),
                r.metadata(),
                r.createdAt()
        );
    }
}
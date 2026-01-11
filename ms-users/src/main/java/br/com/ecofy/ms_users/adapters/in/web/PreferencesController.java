package br.com.ecofy.ms_users.adapters.in.web;

import br.com.ecofy.ms_users.adapters.in.web.dto.UpdatePreferencesRequest;
import br.com.ecofy.ms_users.adapters.in.web.dto.UserPreferencesResponse;
import br.com.ecofy.ms_users.core.application.command.UpdatePreferencesCommand;
import br.com.ecofy.ms_users.core.domain.enums.PreferenceKey;
import br.com.ecofy.ms_users.core.port.in.GetUserPreferencesUseCase;
import br.com.ecofy.ms_users.core.port.in.UpdateUserPreferencesUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(path = "/api/users/v1/preferences", produces = MediaType.APPLICATION_JSON_VALUE)
public class PreferencesController {

    private final UpdateUserPreferencesUseCase updateUseCase;
    private final GetUserPreferencesUseCase getUseCase;

    @PutMapping(path = "/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserPreferencesResponse> update(
            @RequestHeader(name = "Idempotency-Key") String idempotencyKey,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdatePreferencesRequest req
    ) {
        Map<PreferenceKey, String> map = new EnumMap<>(PreferenceKey.class);
        for (Map.Entry<String, String> e : req.preferences().entrySet()) {
            try {
                map.put(PreferenceKey.valueOf(e.getKey()), e.getValue());
            } catch (IllegalArgumentException ex) {
                // ignora chaves desconhecidas (ou altere para 400 conforme sua política)
            }
        }

        var result = updateUseCase.update(new UpdatePreferencesCommand(
                userId, map, idempotencyKey
        ));

        return ResponseEntity.ok(new UserPreferencesResponse(result.userId(), result.preferences()));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserPreferencesResponse> get(@PathVariable UUID userId) {
        var result = getUseCase.getByUserId(userId);
        return ResponseEntity.ok(new UserPreferencesResponse(result.userId(), result.preferences()));
    }
}
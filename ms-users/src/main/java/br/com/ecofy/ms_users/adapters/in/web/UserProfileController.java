package br.com.ecofy.ms_users.adapters.in.web;

import br.com.ecofy.ms_users.adapters.in.web.dto.CreateProfileRequest;
import br.com.ecofy.ms_users.adapters.in.web.dto.UpdateProfileRequest;
import br.com.ecofy.ms_users.adapters.in.web.dto.UserProfileResponse;
import br.com.ecofy.ms_users.core.application.command.CreateUserProfileCommand;
import br.com.ecofy.ms_users.core.application.command.UpdateUserProfileCommand;
import br.com.ecofy.ms_users.core.application.result.UserProfileResult;
import br.com.ecofy.ms_users.core.port.in.CreateUserProfileUseCase;
import br.com.ecofy.ms_users.core.port.in.GetUserProfileUseCase;
import br.com.ecofy.ms_users.core.port.in.UpdateUserProfileUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(path = "/api/users/v1/profile", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserProfileController {

    private final CreateUserProfileUseCase createUseCase;
    private final UpdateUserProfileUseCase updateUseCase;
    private final GetUserProfileUseCase getUseCase;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserProfileResponse> create(
            @RequestHeader(name = "Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateProfileRequest req
    ) {
        var result = createUseCase.create(new CreateUserProfileCommand(
                req.userId(),
                req.externalAuthId(),
                req.fullName(),
                req.email(),
                req.phone(),
                idempotencyKey
        ));
        return ResponseEntity.ok(toResponse(result));
    }

    @PutMapping(path = "/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserProfileResponse> update(
            @RequestHeader(name = "Idempotency-Key") String idempotencyKey,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateProfileRequest req
    ) {
        var result = updateUseCase.update(new UpdateUserProfileCommand(
                userId,
                req.fullName(),
                req.email(),
                req.phone(),
                req.status(),
                idempotencyKey
        ));
        return ResponseEntity.ok(toResponse(result));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> get(@PathVariable UUID userId) {
        var result = getUseCase.getById(userId);
        return ResponseEntity.ok(toResponse(result));
    }

    private static UserProfileResponse toResponse(UserProfileResult r) {
        return new UserProfileResponse(
                r.id(), r.externalAuthId(), r.fullName(), r.email(), r.phone(),
                r.status() != null ? r.status().name() : null,
                r.createdAt(), r.updatedAt()
        );
    }
}

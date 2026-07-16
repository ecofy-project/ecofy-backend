package br.com.ecofy.ms_users.adapters.in.web;

import br.com.ecofy.ms_users.adapters.in.web.dto.request.UpsertUserFromAuthRequest;
import br.com.ecofy.ms_users.core.application.result.UserProfileResult;
import br.com.ecofy.ms_users.core.domain.enums.UserStatus;
import br.com.ecofy.ms_users.core.port.in.UpsertUserFromAuthUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InternalUsersSyncControllerTest {

    @Mock
    private UpsertUserFromAuthUseCase upsertUserFromAuthUseCase;

    @Test
    void upsert_shouldUsePathAsSourceOfTruth_andDelegateToUseCase() {
        var controller = new InternalUsersSyncController(upsertUserFromAuthUseCase);

        UUID id = UUID.randomUUID();
        var expected = new UserProfileResult(
                id, "auth-path-123", "Full Name", "user@ecofy.com", null,
                UserStatus.ACTIVE, true, "en-US", Instant.now(), Instant.now());
        when(upsertUserFromAuthUseCase.upsert(any())).thenReturn(expected);

        // Body traz um authUserId divergente; o PATH deve prevalecer.
        var body = new UpsertUserFromAuthRequest(
                "auth-body-DIVERGENT", "user@ecofy.com", "Full", "Name", null, true, "ACTIVE", "en-US");

        ResponseEntity<UserProfileResult> response = controller.upsert("auth-path-123", body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(expected, response.getBody());

        ArgumentCaptor<UpsertUserFromAuthUseCase.Command> captor =
                ArgumentCaptor.forClass(UpsertUserFromAuthUseCase.Command.class);
        verify(upsertUserFromAuthUseCase).upsert(captor.capture());
        assertEquals("auth-path-123", captor.getValue().authUserId(), "authUserId deve vir do PATH, não do body");
        assertEquals("en-US", captor.getValue().locale());
        assertTrue(captor.getValue().emailVerified());
    }
}

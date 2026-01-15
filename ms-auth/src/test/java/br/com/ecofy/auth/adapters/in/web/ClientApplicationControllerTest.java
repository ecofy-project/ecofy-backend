package br.com.ecofy.auth.adapters.in.web;

import br.com.ecofy.auth.adapters.in.web.dto.request.ClientApplicationRequest;
import br.com.ecofy.auth.adapters.in.web.dto.response.ClientApplicationResponse;
import br.com.ecofy.auth.adapters.in.web.mapper.ClientApplicationMapper;
import br.com.ecofy.auth.core.domain.ClientApplication;
import br.com.ecofy.auth.core.domain.enums.GrantType;
import br.com.ecofy.auth.core.port.in.RegisterClientApplicationUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URI;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientApplicationControllerTest {

    @Mock
    private RegisterClientApplicationUseCase registerClientApplicationUseCase;

    private ClientApplicationController controller;

    @BeforeEach
    void setUp() {
        controller = new ClientApplicationController(registerClientApplicationUseCase);
    }

    private void setupRequestContext(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

    }

    @Test
    void register_shouldDefaultFirstPartyToFalse_whenFirstPartyIsNull() {

        // Arrange
        setupRequestContext("/api/admin/clients");

        // tipos corretos: Set<GrantType>, Set<String>, Set<String>
        Set<GrantType> grantTypes = Set.of(GrantType.AUTHORIZATION_CODE);
        Set<String> redirectUris = Set.of("https://app.example.com/callback");
        Set<String> scopes = Set.of("openid", "profile");

        ClientApplicationRequest request = new ClientApplicationRequest(
                "My Client",
                null,
                grantTypes,
                redirectUris,
                scopes,
                null
        );

        ClientApplication domainClient = mock(ClientApplication.class);
        when(domainClient.clientId()).thenReturn("client-123");
        when(domainClient.clientType()).thenReturn(null);
        when(domainClient.isActive()).thenReturn(true);

        when(registerClientApplicationUseCase.register(any())).thenReturn(domainClient);

        ClientApplicationResponse mappedResponse = mock(ClientApplicationResponse.class);

        try (MockedStatic<ClientApplicationMapper> mapperMock =
                     Mockito.mockStatic(ClientApplicationMapper.class)) {

            mapperMock.when(() -> ClientApplicationMapper.toResponse(domainClient))
                    .thenReturn(mappedResponse);

            // Act
            ResponseEntity<ClientApplicationResponse> response = controller.register(request);

            // Assert
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertSame(mappedResponse, response.getBody(), "Body deve ser o mapeado pelo ClientApplicationMapper");

            URI location = response.getHeaders().getLocation();
            assertNotNull(location, "Location não pode ser nulo");
            assertTrue(location.toString().contains("/api/admin/clients/"),
                    "Location deve conter o path base do endpoint");
            assertTrue(location.toString().endsWith("/client-123"),
                    "Location deve terminar com o clientId retornado");

            ArgumentCaptor<RegisterClientApplicationUseCase.RegisterClientCommand> cmdCaptor =
                    ArgumentCaptor.forClass(RegisterClientApplicationUseCase.RegisterClientCommand.class);

            verify(registerClientApplicationUseCase, times(1)).register(cmdCaptor.capture());

            RegisterClientApplicationUseCase.RegisterClientCommand cmd = cmdCaptor.getValue();
            assertNotNull(cmd);

            assertEquals("My Client", cmd.name());
            assertNull(cmd.clientType());
            assertEquals(grantTypes, cmd.grantTypes());
            assertEquals(redirectUris, cmd.redirectUris());
            assertEquals(scopes, cmd.scopes());
            assertFalse(cmd.firstParty(), "Quando request.firstParty() é null, deve ser false no comando");
        }

    }

    @Test
    void register_shouldUseFirstPartyFromRequest_whenProvidedAsTrue() {

        // Arrange
        setupRequestContext("/api/admin/clients");

        Set<GrantType> grantTypes = Set.of(GrantType.CLIENT_CREDENTIALS);
        Set<String> redirectUris = Set.of(); // vazio
        Set<String> scopes = Set.of("api.read");

        ClientApplicationRequest request = new ClientApplicationRequest(
                "Another Client",
                null,
                grantTypes,
                redirectUris,
                scopes,
                Boolean.TRUE
        );

        ClientApplication domainClient = mock(ClientApplication.class);
        when(domainClient.clientId()).thenReturn("client-999");
        when(domainClient.clientType()).thenReturn(null);
        when(domainClient.isActive()).thenReturn(true);

        when(registerClientApplicationUseCase.register(any())).thenReturn(domainClient);

        ClientApplicationResponse mappedResponse = mock(ClientApplicationResponse.class);

        try (MockedStatic<ClientApplicationMapper> mapperMock =
                     Mockito.mockStatic(ClientApplicationMapper.class)) {

            mapperMock.when(() -> ClientApplicationMapper.toResponse(domainClient))
                    .thenReturn(mappedResponse);

            // Act
            ResponseEntity<ClientApplicationResponse> response = controller.register(request);

            // Assert
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertSame(mappedResponse, response.getBody());

            URI location = response.getHeaders().getLocation();
            assertNotNull(location);
            assertTrue(location.toString().endsWith("/client-999"));

            ArgumentCaptor<RegisterClientApplicationUseCase.RegisterClientCommand> cmdCaptor =
                    ArgumentCaptor.forClass(RegisterClientApplicationUseCase.RegisterClientCommand.class);

            verify(registerClientApplicationUseCase).register(cmdCaptor.capture());

            RegisterClientApplicationUseCase.RegisterClientCommand cmd = cmdCaptor.getValue();
            assertNotNull(cmd);
            assertEquals("Another Client", cmd.name());
            assertEquals(grantTypes, cmd.grantTypes());
            assertEquals(redirectUris, cmd.redirectUris());
            assertEquals(scopes, cmd.scopes());
            assertTrue(cmd.firstParty(), "Quando request.firstParty() é true, o comando deve refletir true");

        }

    }

}

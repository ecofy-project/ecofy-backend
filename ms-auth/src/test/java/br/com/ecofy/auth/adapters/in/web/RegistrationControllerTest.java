package br.com.ecofy.auth.adapters.in.web;

import br.com.ecofy.auth.adapters.in.web.dto.request.ConfirmEmailRequest;
import br.com.ecofy.auth.adapters.in.web.dto.request.RegisterUserRequest;
import br.com.ecofy.auth.adapters.in.web.dto.response.UserResponse;
import br.com.ecofy.auth.adapters.in.web.mapper.UserMapper;
import br.com.ecofy.auth.core.domain.AuthUser;
import br.com.ecofy.auth.core.port.in.ConfirmEmailUseCase;
import br.com.ecofy.auth.core.port.in.RegisterUserUseCase;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationControllerTest {

    @Mock
    private RegisterUserUseCase registerUserUseCase;

    @Mock
    private ConfirmEmailUseCase confirmEmailUseCase;

    private RegistrationController controller;

    @BeforeEach
    void setUp() {
        controller = new RegistrationController(registerUserUseCase, confirmEmailUseCase);
    }

    private void setupRequestContext(String contextPath) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath(contextPath);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }


    @Test
    void register_shouldUseDefaultLocaleAndAuthUserRole_whenLocaleIsNull() {

        // Arrange
        setupRequestContext(""); // para ServletUriComponentsBuilder.fromCurrentContextPath()

        RegisterUserRequest request = new RegisterUserRequest(
                "user@example.com",
                "StrongPass!23",
                "John",
                "Doe",
                null
        );

        AuthUser domainUser = mock(AuthUser.class, Answers.RETURNS_DEEP_STUBS);
        when(domainUser.id().value()).thenReturn(UUID.randomUUID());
        when(domainUser.email().value()).thenReturn("user@example.com");

        when(registerUserUseCase.register(any())).thenReturn(domainUser);

        UserResponse mappedResponse = mock(UserResponse.class);

        try (MockedStatic<UserMapper> mapperMock = Mockito.mockStatic(UserMapper.class)) {

            mapperMock.when(() -> UserMapper.toResponse(domainUser))
                    .thenReturn(mappedResponse);

            // Act
            ResponseEntity<UserResponse> response = controller.register(request);

            // Assert HTTP
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertSame(mappedResponse, response.getBody());

            URI location = response.getHeaders().getLocation();
            assertNotNull(location, "Location não pode ser nulo");
            assertTrue(location.toString().endsWith("/api/user/me"),
                    "Location deve apontar para /api/user/me");

            // Captura do comando
            ArgumentCaptor<RegisterUserUseCase.RegisterUserCommand> cmdCaptor =
                    ArgumentCaptor.forClass(RegisterUserUseCase.RegisterUserCommand.class);

            verify(registerUserUseCase, times(1)).register(cmdCaptor.capture());

            RegisterUserUseCase.RegisterUserCommand cmd = cmdCaptor.getValue();
            assertNotNull(cmd);
            assertEquals("user@example.com", cmd.email());
            assertEquals("StrongPass!23", cmd.rawPassword());
            assertEquals("John", cmd.firstName());
            assertEquals("Doe", cmd.lastName());
            assertEquals("pt-BR", cmd.locale(), "Locale deve ser 'pt-BR' quando request.locale() é null");
            assertFalse(cmd.autoConfirmEmail(), "O controller envia false para emailConfirmed");
            assertEquals(List.of("AUTH_USER"), cmd.roles(), "Roles padrão devem ser AUTH_USER");
        }

    }

    @Test
    void register_shouldUseProvidedLocale_whenLocaleIsProvided() {

        // Arrange
        setupRequestContext("");

        RegisterUserRequest request = new RegisterUserRequest(
                "another@example.com",
                "AnotherPass!42",
                "Jane",
                "Smith",
                "en-US" // locale explícito
        );

        AuthUser domainUser = mock(AuthUser.class, Answers.RETURNS_DEEP_STUBS);
        when(domainUser.id().value()).thenReturn(UUID.randomUUID());
        when(domainUser.email().value()).thenReturn("another@example.com");

        when(registerUserUseCase.register(any())).thenReturn(domainUser);

        UserResponse mappedResponse = mock(UserResponse.class);

        try (MockedStatic<UserMapper> mapperMock = Mockito.mockStatic(UserMapper.class)) {

            mapperMock.when(() -> UserMapper.toResponse(domainUser))
                    .thenReturn(mappedResponse);

            // Act
            ResponseEntity<UserResponse> response = controller.register(request);

            // Assert HTTP
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertSame(mappedResponse, response.getBody());

            URI location = response.getHeaders().getLocation();
            assertNotNull(location);
            assertTrue(location.toString().endsWith("/api/user/me"));

            // Captura do comando
            ArgumentCaptor<RegisterUserUseCase.RegisterUserCommand> cmdCaptor =
                    ArgumentCaptor.forClass(RegisterUserUseCase.RegisterUserCommand.class);

            verify(registerUserUseCase).register(cmdCaptor.capture());

            RegisterUserUseCase.RegisterUserCommand cmd = cmdCaptor.getValue();
            assertNotNull(cmd);
            assertEquals("another@example.com", cmd.email());
            assertEquals("AnotherPass!42", cmd.rawPassword());
            assertEquals("Jane", cmd.firstName());
            assertEquals("Smith", cmd.lastName());
            assertEquals("en-US", cmd.locale(), "Locale deve ser o do request quando não é null");
            assertFalse(cmd.autoConfirmEmail());
            assertEquals(List.of("AUTH_USER"), cmd.roles());
        }

    }


    @Test
    void confirmEmail_shouldCallUseCaseAndReturnOkWithMappedUser() {

        // Arrange
        ConfirmEmailRequest request = new ConfirmEmailRequest("token-123");

        AuthUser domainUser = mock(AuthUser.class, Answers.RETURNS_DEEP_STUBS);
        when(domainUser.id().value()).thenReturn(UUID.randomUUID());
        when(domainUser.email().value()).thenReturn("confirmed@example.com");

        when(confirmEmailUseCase.confirm(any())).thenReturn(domainUser);

        UserResponse mappedResponse = mock(UserResponse.class);

        try (MockedStatic<UserMapper> mapperMock = Mockito.mockStatic(UserMapper.class)) {

            mapperMock.when(() -> UserMapper.toResponse(domainUser))
                    .thenReturn(mappedResponse);

            // Act
            ResponseEntity<UserResponse> response = controller.confirmEmail(request);

            // Assert
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertSame(mappedResponse, response.getBody());

            ArgumentCaptor<ConfirmEmailUseCase.ConfirmEmailCommand> cmdCaptor =
                    ArgumentCaptor.forClass(ConfirmEmailUseCase.ConfirmEmailCommand.class);

            verify(confirmEmailUseCase, times(1)).confirm(cmdCaptor.capture());

            ConfirmEmailUseCase.ConfirmEmailCommand cmd = cmdCaptor.getValue();
            assertNotNull(cmd);
            assertEquals("token-123", cmd.verificationToken());
        }

    }

}

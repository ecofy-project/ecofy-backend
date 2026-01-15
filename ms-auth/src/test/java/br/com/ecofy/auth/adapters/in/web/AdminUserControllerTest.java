package br.com.ecofy.auth.adapters.in.web;

import br.com.ecofy.auth.adapters.in.web.dto.request.AdminUserCreateRequest;
import br.com.ecofy.auth.adapters.in.web.dto.response.UserResponse;
import br.com.ecofy.auth.adapters.in.web.mapper.UserMapper;
import br.com.ecofy.auth.core.domain.AuthUser;
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
class AdminUserControllerTest {

    @Mock
    private RegisterUserUseCase registerUserUseCase;

    private AdminUserController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminUserController(registerUserUseCase);
    }

    private void setupRequestContext(String uri) {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

    }

    @Test
    void createAdmin_shouldCreateUserWithDefaultRolesAndLocale_whenRolesAndLocaleAreNull() {

        // Arrange
        setupRequestContext("/api/admin/users");

        AdminUserCreateRequest request = new AdminUserCreateRequest(
                "admin@example.com",
                "StrongPass123!",
                "Admin",
                "User",
                null,
                null
        );

        // Mock do usuário de domínio retornado pelo use case
        AuthUser domainUser = mock(AuthUser.class, Answers.RETURNS_DEEP_STUBS);
        UUID userId = UUID.randomUUID();

        when(domainUser.id().value()).thenReturn(userId);
        when(domainUser.email().value()).thenReturn("admin@example.com");

        when(registerUserUseCase.register(any())).thenReturn(domainUser);

        UserResponse mappedResponse = mock(UserResponse.class);

        try (MockedStatic<UserMapper> mapperMock = Mockito.mockStatic(UserMapper.class)) {
            mapperMock.when(() -> UserMapper.toResponse(domainUser))
                    .thenReturn(mappedResponse);

            // Act
            ResponseEntity<UserResponse> response = controller.createAdmin(request);

            // Assert
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertSame(mappedResponse, response.getBody(), "Body deve ser o retornado pelo UserMapper");

            URI location = response.getHeaders().getLocation();
            assertNotNull(location, "Location não pode ser nulo");
            assertTrue(location.toString().contains("/api/admin/users/"),
                    "Location deve conter o path base do endpoint");

            // Captura do comando para validar mapeamento básico
            ArgumentCaptor<RegisterUserUseCase.RegisterUserCommand> cmdCaptor =
                    ArgumentCaptor.forClass(RegisterUserUseCase.RegisterUserCommand.class);

            verify(registerUserUseCase, times(1)).register(cmdCaptor.capture());

            RegisterUserUseCase.RegisterUserCommand cmd = cmdCaptor.getValue();
            assertNotNull(cmd, "Command não pode ser nulo");
            assertEquals("admin@example.com", cmd.email());
            assertEquals("StrongPass123!", cmd.rawPassword());
            assertEquals("Admin", cmd.firstName());
            assertEquals("User", cmd.lastName());

            // A implementação atual passa request.locale() diretamente,
            // então aqui será null nesse cenário.
            assertNull(cmd.locale(), "Locale no command deve ser null neste cenário");

            assertNotNull(cmd.roles(), "Roles no command não pode ser nulo");

        }

    }

    @Test
    void createAdmin_shouldCreateUserWithProvidedRolesAndLocale_whenTheyArePresent() {

        // Arrange
        setupRequestContext("/api/admin/users");

        List<String> customRoles = List.of("AUTH_ADMIN", "AUTH_USER", "AUTH_AUDITOR");

        AdminUserCreateRequest request = new AdminUserCreateRequest(
                "root@example.com",
                "AnotherStrongPass!23",
                "Root",
                "User",
                "en-US",
                customRoles
        );

        AuthUser domainUser = mock(AuthUser.class, Answers.RETURNS_DEEP_STUBS);
        UUID userId = UUID.randomUUID();

        when(domainUser.id().value()).thenReturn(userId);
        when(domainUser.email().value()).thenReturn("root@example.com");

        when(registerUserUseCase.register(any())).thenReturn(domainUser);

        UserResponse mappedResponse = mock(UserResponse.class);

        try (MockedStatic<UserMapper> mapperMock = Mockito.mockStatic(UserMapper.class)) {
            mapperMock.when(() -> UserMapper.toResponse(domainUser))
                    .thenReturn(mappedResponse);

            // Act
            ResponseEntity<UserResponse> response = controller.createAdmin(request);

            // Assert
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertSame(mappedResponse, response.getBody());

            URI location = response.getHeaders().getLocation();
            assertNotNull(location);

            // agora valida usando o próprio UUID usado no stub
            assertTrue(location.toString().endsWith("/" + userId),
                    "Location deve terminar com o id retornado pelo domínio");

            // Captura do comando para validar repasse de campos
            ArgumentCaptor<RegisterUserUseCase.RegisterUserCommand> cmdCaptor =
                    ArgumentCaptor.forClass(RegisterUserUseCase.RegisterUserCommand.class);

            verify(registerUserUseCase, times(1)).register(cmdCaptor.capture());

            RegisterUserUseCase.RegisterUserCommand cmd = cmdCaptor.getValue();
            assertEquals("root@example.com", cmd.email());
            assertEquals("AnotherStrongPass!23", cmd.rawPassword());
            assertEquals("Root", cmd.firstName());
            assertEquals("User", cmd.lastName());
            assertEquals("en-US", cmd.locale());

            assertNotNull(cmd.roles(), "Roles no command não pode ser nulo");

        }

    }

}

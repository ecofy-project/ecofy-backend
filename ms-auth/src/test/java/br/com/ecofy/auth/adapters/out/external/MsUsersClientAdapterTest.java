package br.com.ecofy.auth.adapters.out.external;

import br.com.ecofy.auth.config.UsersMsProperties;
import br.com.ecofy.auth.core.domain.AuthUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MsUsersClientAdapterTest {

    @Mock
    private RestClient msUsersRestClient;

    @Mock
    private UsersMsProperties props;

    private MsUsersClientAdapter adapter;

    private RestClient.RequestBodyUriSpec putSpec;
    private RestClient.RequestBodySpec bodySpec;

    @SuppressWarnings("unchecked")
    private RestClient.RequestHeadersSpec<?> headersSpec;

    private RestClient.ResponseSpec responseSpec;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        adapter = new MsUsersClientAdapter(msUsersRestClient, props);

        putSpec = mock(RestClient.RequestBodyUriSpec.class);
        bodySpec = mock(RestClient.RequestBodySpec.class);
        headersSpec = (RestClient.RequestHeadersSpec<?>) mock(RestClient.RequestHeadersSpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);
    }

    @Test
    void upsertUser_whenDisabled_shouldReturnWithoutCallingRestClient_andAllowNullUser() {
        when(props.enabled()).thenReturn(false);

        assertThatCode(() -> adapter.upsertUser(null)).doesNotThrowAnyException();

        verify(props).enabled();
        verifyNoMoreInteractions(props);
        verifyNoInteractions(msUsersRestClient);
    }

    @Test
    void upsertUser_whenEnabledAndLocaleNull_shouldSendPutWithDefaultLocalePtBR() throws Exception {
        when(props.enabled()).thenReturn(true);
        when(props.internalToken()).thenReturn("token-123");

        when(msUsersRestClient.put()).thenReturn(putSpec);
        when(putSpec.uri(eq("/internal/users/{authUserId}"), anyString())).thenReturn(bodySpec);
        when(bodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(bodySpec);
        when(bodySpec.header(eq("X-Internal-Token"), eq("token-123"))).thenReturn(bodySpec);

        doAnswer(inv -> bodySpec).when(bodySpec).body(any(Object.class));

        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.ok().build());

        UUID userId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        AuthUser user = mock(AuthUser.class, Answers.RETURNS_DEEP_STUBS);
        when(user.id().value()).thenReturn(userId);
        when(user.email().value()).thenReturn("admin@example.com");
        when(user.firstName()).thenReturn("Admin");
        when(user.lastName()).thenReturn("User");
        when(user.isEmailVerified()).thenReturn(true);
        when(user.status().name()).thenReturn("ACTIVE");
        when(user.locale()).thenReturn(null);

        adapter.upsertUser(user);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);

        verify(props).enabled();
        verify(props).internalToken();

        verify(msUsersRestClient).put();
        verify(putSpec).uri("/internal/users/{authUserId}", userId.toString());
        verify(bodySpec).contentType(MediaType.APPLICATION_JSON);
        verify(bodySpec).header("X-Internal-Token", "token-123");
        verify(bodySpec).body(payloadCaptor.capture());
        verify(bodySpec).retrieve();
        verify(responseSpec).toBodilessEntity();
        verifyNoMoreInteractions(msUsersRestClient);

        Object payload = payloadCaptor.getValue();
        assertThat(readRecordComponent(payload, "authUserId")).isEqualTo(userId.toString());
        assertThat(readRecordComponent(payload, "email")).isEqualTo("admin@example.com");
        assertThat(readRecordComponent(payload, "firstName")).isEqualTo("Admin");
        assertThat(readRecordComponent(payload, "lastName")).isEqualTo("User");
        assertThat(readRecordComponent(payload, "fullName")).isEqualTo("Admin User");
        assertThat(readRecordComponent(payload, "emailVerified")).isEqualTo(true);
        assertThat(readRecordComponent(payload, "status")).isEqualTo("ACTIVE");
        assertThat(readRecordComponent(payload, "locale")).isEqualTo("pt-BR");
    }

    @Test
    void upsertUser_whenEnabledAndLocaleProvided_shouldSendPutWithProvidedLocale() throws Exception {
        when(props.enabled()).thenReturn(true);
        when(props.internalToken()).thenReturn("token-xyz");

        when(msUsersRestClient.put()).thenReturn(putSpec);
        when(putSpec.uri(eq("/internal/users/{authUserId}"), anyString())).thenReturn(bodySpec);
        when(bodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(bodySpec);
        when(bodySpec.header(eq("X-Internal-Token"), eq("token-xyz"))).thenReturn(bodySpec);

        doAnswer(inv -> bodySpec).when(bodySpec).body(any(Object.class));

        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.accepted().build());

        UUID userId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        AuthUser user = mock(AuthUser.class, Answers.RETURNS_DEEP_STUBS);
        when(user.id().value()).thenReturn(userId);
        when(user.email().value()).thenReturn("root@example.com");
        when(user.firstName()).thenReturn("Root");
        when(user.lastName()).thenReturn("User");
        when(user.isEmailVerified()).thenReturn(false);
        when(user.status().name()).thenReturn("PENDING");
        when(user.locale()).thenReturn("en-US");

        adapter.upsertUser(user);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);

        verify(props).enabled();
        verify(props).internalToken();

        verify(msUsersRestClient).put();
        verify(putSpec).uri("/internal/users/{authUserId}", userId.toString());
        verify(bodySpec).contentType(MediaType.APPLICATION_JSON);
        verify(bodySpec).header("X-Internal-Token", "token-xyz");
        verify(bodySpec).body(payloadCaptor.capture());
        verify(bodySpec).retrieve();
        verify(responseSpec).toBodilessEntity();

        Object payload = payloadCaptor.getValue();
        assertThat(readRecordComponent(payload, "authUserId")).isEqualTo(userId.toString());
        assertThat(readRecordComponent(payload, "email")).isEqualTo("root@example.com");
        assertThat(readRecordComponent(payload, "fullName")).isEqualTo("Root User");
        assertThat(readRecordComponent(payload, "emailVerified")).isEqualTo(false);
        assertThat(readRecordComponent(payload, "status")).isEqualTo("PENDING");
        assertThat(readRecordComponent(payload, "locale")).isEqualTo("en-US");
    }

    @Test
    void upsertUser_whenEnabledAndRestClientThrows_shouldNotPropagateException() {
        when(props.enabled()).thenReturn(true);
        when(props.internalToken()).thenReturn("token-err");

        when(msUsersRestClient.put()).thenReturn(putSpec);
        when(putSpec.uri(eq("/internal/users/{authUserId}"), anyString())).thenReturn(bodySpec);
        when(bodySpec.contentType(MediaType.APPLICATION_JSON)).thenReturn(bodySpec);
        when(bodySpec.header(eq("X-Internal-Token"), eq("token-err"))).thenReturn(bodySpec);

        doAnswer(inv -> bodySpec).when(bodySpec).body(any(Object.class));

        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenThrow(new RuntimeException("boom"));

        UUID userId = UUID.fromString("00000000-1111-2222-3333-444444444444");
        AuthUser user = mock(AuthUser.class, Answers.RETURNS_DEEP_STUBS);
        when(user.id().value()).thenReturn(userId);
        when(user.email().value()).thenReturn("x@example.com");
        when(user.firstName()).thenReturn("X");
        when(user.lastName()).thenReturn("Y");
        when(user.isEmailVerified()).thenReturn(true);
        when(user.status().name()).thenReturn("ACTIVE");
        when(user.locale()).thenReturn("pt-BR");

        assertThatCode(() -> adapter.upsertUser(user)).doesNotThrowAnyException();

        verify(props).enabled();
        verify(props).internalToken();

        verify(msUsersRestClient).put();
        verify(putSpec).uri("/internal/users/{authUserId}", userId.toString());
        verify(bodySpec).contentType(MediaType.APPLICATION_JSON);
        verify(bodySpec).header("X-Internal-Token", "token-err");
        verify(bodySpec).body(any(Object.class));
        verify(bodySpec).retrieve();
        verify(responseSpec).toBodilessEntity();
    }

    private static Object readRecordComponent(Object record, String componentName) throws Exception {
        Method m = record.getClass().getDeclaredMethod(componentName);
        m.setAccessible(true);
        return m.invoke(record);
    }
}
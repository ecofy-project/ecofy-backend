package br.com.ecofy.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HttpClientsConfigTest {

    @Test
    void msUsersRestClient_shouldCallPropsBaseUrl_buildClient_andThrowOnNullProps() {
        HttpClientsConfig config = new HttpClientsConfig();

        assertThrows(NullPointerException.class, () -> config.msUsersRestClient(null));

        UsersMsProperties props = mock(UsersMsProperties.class);
        when(props.baseUrl()).thenReturn("http://localhost:8081");

        RestClient client = config.msUsersRestClient(props);

        assertNotNull(client);

        verify(props, times(1)).baseUrl();
        verifyNoMoreInteractions(props);
    }

    private static String extractBaseUrl(RestClient client) throws Exception {
        for (String field : new String[]{"baseUrl", "baseUri", "uriBuilderFactory", "uriTemplateHandler"}) {
            try {
                var f = client.getClass().getDeclaredField(field);
                f.setAccessible(true);
                Object v = f.get(client);
                if (v == null) continue;

                if (v instanceof String s && !s.isBlank()) return s;

                String fromToString = v.toString();
                if (fromToString.contains("http://") || fromToString.contains("https://")) {
                    return fromToString;
                }
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }
}
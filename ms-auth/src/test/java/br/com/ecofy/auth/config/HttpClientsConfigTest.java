package br.com.ecofy.auth.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários da configuração dos clientes HTTP")
class HttpClientsConfigTest {

    @Mock
    private UsersMsProperties usersMsProperties;

    @Test
    @DisplayName("Deve criar o cliente HTTP do ms-users com a URL base configurada")
    void msUsersRestClient_propriedadesValidas_deveRetornarRestClientConfigurado() {
        // Arrange
        String baseUrl = "http://localhost:8081";
        when(usersMsProperties.baseUrl()).thenReturn(baseUrl);

        HttpClientsConfig config = new HttpClientsConfig();

        // Act
        RestClient result =
                config.msUsersRestClient(usersMsProperties);

        // Assert
        assertNotNull(result);

        verify(usersMsProperties).baseUrl();
    }

    @Test
    @DisplayName("Deve lançar exceção quando as propriedades do ms-users forem nulas")
    void msUsersRestClient_propriedadesNulas_deveLancarNullPointerException() {
        // Arrange
        HttpClientsConfig config = new HttpClientsConfig();

        // Act
        assertThrows(
                NullPointerException.class,
                () -> config.msUsersRestClient(null)
        );

        // Assert
        verifyNoInteractions(usersMsProperties);
    }
}

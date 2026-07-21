package br.com.ecofy.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

// Configura os clientes HTTP utilizados nas integrações externas.
@Configuration
public class HttpClientsConfig {

    // Registra o cliente HTTP responsável pela comunicação com o ms-users.
    @Bean
    public RestClient msUsersRestClient(UsersMsProperties props) {
        return RestClient.builder()
                .baseUrl(props.baseUrl())
                .build();
    }
}

package br.com.ecofy.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpClientsConfig {

    @Bean
    public RestClient msUsersRestClient(UsersMsProperties props) {
        return RestClient.builder()
                .baseUrl(props.baseUrl())
                .build();
    }

}
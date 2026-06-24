package br.com.ecofy.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(UsersMsProperties.class)
public class PropertiesConfig {

}
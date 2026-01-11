package br.com.ecofy.ms_notification;

import br.com.ecofy.ms_notification.config.NotificationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableConfigurationProperties(NotificationProperties.class)
public class MsNotificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsNotificationApplication.class, args);
    }
}

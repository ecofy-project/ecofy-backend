package br.com.ecofy.gateway.api_gateway.config;

import br.com.ecofy.gateway.api_gateway.core.port.out.PublishAccessLogPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AccessLogPublisherConfig {

    private static final Logger log = LoggerFactory.getLogger(AccessLogPublisherConfig.class);

    @Bean
    @ConditionalOnMissingBean(PublishAccessLogPort.class)
    public PublishAccessLogPort noopPublishAccessLogPort() {
        return entry -> log.debug("[NoopPublishAccessLogPort] Access log skipped. path={}", entry.path());
    }
}

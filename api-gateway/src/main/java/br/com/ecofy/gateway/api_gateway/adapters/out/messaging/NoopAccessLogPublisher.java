package br.com.ecofy.gateway.api_gateway.adapters.out.messaging;

import br.com.ecofy.gateway.api_gateway.core.port.out.PublishAccessLogPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(PublishAccessLogPort.class)
public class NoopAccessLogPublisher implements PublishAccessLogPort {

    private static final Logger log = LoggerFactory.getLogger(NoopAccessLogPublisher.class);

    @Override
    public void publishAccessLog(AccessLogEntry entry) {
        // Não faz nada (ou loga debug)
        log.debug("[NoopAccessLogPublisher] Access log skipped (Kafka disabled). path={}", entry.path());
    }
}
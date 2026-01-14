package br.com.ecofy.gateway.api_gateway.core.application.service;

import br.com.ecofy.gateway.api_gateway.core.domain.TenantContext;
import br.com.ecofy.gateway.api_gateway.core.port.out.PublishAccessLogPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Map;

@Service
public class GatewayLoggingService {

    private static final Logger log = LoggerFactory.getLogger(GatewayLoggingService.class);

    private final PublishAccessLogPort publisher;

    public GatewayLoggingService(PublishAccessLogPort publisher) {
        this.publisher = publisher;
    }

    public void logAccess(
            TenantContext ctx,
            String httpMethod,
            String path,
            String query,
            int statusCode,
            long latencyMs,
            String upstreamServiceId,
            InetSocketAddress remoteAddress,
            Map<String, String> extraTags
    ) {
        String clientIp = (remoteAddress != null && remoteAddress.getAddress() != null)
                ? remoteAddress.getAddress().getHostAddress()
                : "unknown";

        Map<String, String> safeTags = (extraTags == null) ? Map.of() : Map.copyOf(extraTags);

        PublishAccessLogPort.AccessLogEntry entry = new PublishAccessLogPort.AccessLogEntry(
                ctx,
                httpMethod,
                path,
                query,
                statusCode,
                latencyMs,
                upstreamServiceId,
                clientIp,
                Instant.now(),
                safeTags
        );

        try {
            publisher.publishAccessLog(entry);
        } catch (Exception ex) {
            log.warn("[GatewayLoggingService] Failed to publish access log: {}", ex.getMessage(), ex);
        }
    }
}

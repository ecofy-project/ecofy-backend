package br.com.ecofy.gateway.api_gateway.core.port.out;

import br.com.ecofy.gateway.api_gateway.core.domain.TenantContext;

import java.time.Instant;
import java.util.Map;

public interface PublishAccessLogPort {

    void publishAccessLog(AccessLogEntry entry);

    record AccessLogEntry(
            TenantContext tenantContext,
            String httpMethod,
            String path,
            String query,
            int statusCode,
            long latencyMs,
            String upstreamServiceId,
            String clientIp,
            Instant timestamp,
            Map<String, String> extraTags
    ) {}
}

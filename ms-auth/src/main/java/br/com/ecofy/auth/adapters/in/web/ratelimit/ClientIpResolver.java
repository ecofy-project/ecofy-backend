package br.com.ecofy.auth.adapters.in.web.ratelimit;

import br.com.ecofy.auth.config.RateLimitProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.stereotype.Component;

// Resolve o IP do cliente considerando apenas proxies previamente confiáveis.
@Component
public class ClientIpResolver {

    private static final String XFF = "X-Forwarded-For";
    private static final String UNKNOWN = "unknown";

    private final List<String> trustedProxyPrefixes;

    public ClientIpResolver(RateLimitProperties properties) {
        this.trustedProxyPrefixes = properties.getTrustedProxies();
    }

    // Seleciona o IP encaminhado pelo proxy ou utiliza o endereço remoto da conexão.
    public String resolve(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();

        if (isTrustedProxy(remoteAddr)) {
            String forwarded = request.getHeader(XFF);
            String client = firstEntry(forwarded);

            if (client != null) {
                return client;
            }
        }

        return (remoteAddr == null || remoteAddr.isBlank())
                ? UNKNOWN
                : remoteAddr;
    }

    private boolean isTrustedProxy(String remoteAddr) {
        if (remoteAddr == null || remoteAddr.isBlank()) {
            return false;
        }

        return trustedProxyPrefixes.stream()
                .filter(p -> p != null && !p.isBlank())
                .anyMatch(prefix -> remoteAddr.startsWith(prefix.trim()));
    }

    // Valida e extrai o primeiro endereço informado pelo proxy.
    private String firstEntry(String forwarded) {
        if (forwarded == null || forwarded.isBlank()) {
            return null;
        }

        String first = forwarded.split(",")[0].trim();

        if (first.isEmpty()
                || first.length() > 45
                || !first.matches("^[A-Za-z0-9.:%_\\[\\]-]+$")) {
            return null;
        }

        return first;
    }
}

package br.com.ecofy.auth.core.port.out;

import br.com.ecofy.auth.core.domain.ratelimit.RateLimitDecision;
import br.com.ecofy.auth.core.domain.ratelimit.RateLimitPolicy;

// Aplica rate limiting distribuído consumindo cota por chave lógica sob uma política.
public interface RateLimiterPort {

    // Consome uma unidade de cota para a chave sob a política informada.
    RateLimitDecision tryConsume(String key, RateLimitPolicy policy);
}

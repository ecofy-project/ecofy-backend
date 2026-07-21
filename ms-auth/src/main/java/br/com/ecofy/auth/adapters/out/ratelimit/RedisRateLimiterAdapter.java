package br.com.ecofy.auth.adapters.out.ratelimit;

import br.com.ecofy.auth.core.domain.ratelimit.RateLimitDecision;
import br.com.ecofy.auth.core.domain.ratelimit.RateLimitPolicy;
import br.com.ecofy.auth.core.port.out.RateLimiterPort;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

// Aplica limites distribuídos de requisição utilizando janelas deslizantes no Redis.
@Component
@Slf4j
public class RedisRateLimiterAdapter implements RateLimiterPort {

    private static final String KEY_PREFIX = "ecofy:auth:rl:";

    // Controla a admissão e calcula o tempo restante de forma atômica.
    private static final String SLIDING_WINDOW_LUA = """
            local now = tonumber(ARGV[1])
            local window = tonumber(ARGV[2])
            local limit = tonumber(ARGV[3])
            redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, now - window)
            local count = redis.call('ZCARD', KEYS[1])
            if count < limit then
              redis.call('ZADD', KEYS[1], now, ARGV[4])
              redis.call('PEXPIRE', KEYS[1], window)
              return -1
            end
            local oldest = redis.call('ZRANGE', KEYS[1], 0, 0, 'WITHSCORES')
            local retry = (tonumber(oldest[2]) + window) - now
            if retry < 0 then retry = 0 end
            return math.floor(retry)
            """;

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<Long> script;
    private final Clock clock;
    private final MeterRegistry meterRegistry;

    public RedisRateLimiterAdapter(
            StringRedisTemplate redisTemplate,
            Clock clock,
            MeterRegistry meterRegistry
    ) {
        this.redisTemplate = redisTemplate;
        this.clock = clock;
        this.meterRegistry = meterRegistry;
        this.script = new DefaultRedisScript<>(
                SLIDING_WINDOW_LUA,
                Long.class
        );
    }

    // Avalia e registra o consumo conforme a política recebida.
    @Override
    public RateLimitDecision tryConsume(
            String key,
            RateLimitPolicy policy
    ) {
        long now = clock.millis();
        long windowMs = policy.window().toMillis();
        String member = now + "-" + UUID.randomUUID();

        try {
            Long retryAfterMs = redisTemplate.execute(
                    script,
                    List.of(KEY_PREFIX + key),
                    Long.toString(now),
                    Long.toString(windowMs),
                    Integer.toString(policy.limit()),
                    member
            );

            if (retryAfterMs == null || retryAfterMs < 0) {
                return RateLimitDecision.allow();
            }

            return RateLimitDecision.deny(
                    Duration.ofMillis(retryAfterMs)
            );
        } catch (RuntimeException ex) {
            meterRegistry.counter(
                    "ecofy.auth.rate_limit.error",
                    "policy",
                    policy.name()
            ).increment();

            log.error(
                    "[RedisRateLimiterAdapter] - [tryConsume] -> Redis indisponível; permitindo requisição (fail-open) policy={} error={}",
                    policy.name(),
                    ex.getMessage()
            );

            return RateLimitDecision.allow();
        }
    }
}

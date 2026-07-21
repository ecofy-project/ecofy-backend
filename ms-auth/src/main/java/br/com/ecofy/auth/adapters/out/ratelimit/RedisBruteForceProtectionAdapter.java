package br.com.ecofy.auth.adapters.out.ratelimit;

import br.com.ecofy.auth.config.BruteForceProperties;
import br.com.ecofy.auth.core.domain.bruteforce.BlockStatus;
import br.com.ecofy.auth.core.port.out.BruteForceProtectionPort;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

// Aplica bloqueios temporários e progressivos contra tentativas repetidas de autenticação.
@Component
@Slf4j
public class RedisBruteForceProtectionAdapter implements BruteForceProtectionPort {

    private static final String FAIL_PREFIX = "ecofy:auth:bf:fail:";
    private static final String BLOCK_PREFIX = "ecofy:auth:bf:block:";
    private static final String LEVEL_PREFIX = "ecofy:auth:bf:level:";

    // Registra falhas e aplica bloqueios progressivos de forma atômica.
    private static final String REGISTER_FAILURE_LUA = """
            local fails = redis.call('INCR', KEYS[1])
            if fails == 1 then
              redis.call('PEXPIRE', KEYS[1], ARGV[2])
            end
            if fails < tonumber(ARGV[1]) then
              return 0
            end
            local level = redis.call('INCR', KEYS[3])
            redis.call('PEXPIRE', KEYS[3], ARGV[5])
            local dur = tonumber(ARGV[3]) * (tonumber(ARGV[4]) ^ (level - 1))
            local maxDur = tonumber(ARGV[5])
            if dur > maxDur then dur = maxDur end
            dur = math.floor(dur)
            redis.call('SET', KEYS[2], '1', 'PX', dur)
            redis.call('DEL', KEYS[1])
            return dur
            """;

    private final StringRedisTemplate redisTemplate;
    private final BruteForceProperties properties;
    private final RedisScript<Long> registerFailureScript;
    private final MeterRegistry meterRegistry;

    public RedisBruteForceProtectionAdapter(
            StringRedisTemplate redisTemplate,
            BruteForceProperties properties,
            MeterRegistry meterRegistry
    ) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
        this.registerFailureScript = new DefaultRedisScript<>(
                REGISTER_FAILURE_LUA,
                Long.class
        );
    }

    // Consulta o bloqueio vigente e o seu tempo restante.
    @Override
    public BlockStatus status(String key) {
        try {
            Long ttlMs = redisTemplate.getExpire(
                    BLOCK_PREFIX + key,
                    TimeUnit.MILLISECONDS
            );

            if (ttlMs == null || ttlMs < 0) {
                return BlockStatus.notBlocked();
            }

            return BlockStatus.blockedFor(Duration.ofMillis(ttlMs));
        } catch (RuntimeException ex) {
            return failOpen("status", ex);
        }
    }

    // Registra uma falha e aplica o bloqueio progressivo quando necessário.
    @Override
    public BlockStatus registerFailure(String key) {
        try {
            Long blockMs = redisTemplate.execute(
                    registerFailureScript,
                    List.of(
                            FAIL_PREFIX + key,
                            BLOCK_PREFIX + key,
                            LEVEL_PREFIX + key
                    ),
                    Integer.toString(properties.getThreshold()),
                    Long.toString(
                            properties.getObservationWindow().toMillis()
                    ),
                    Long.toString(
                            properties.getInitialBlock().toMillis()
                    ),
                    Double.toString(properties.getMultiplier()),
                    Long.toString(
                            properties.getMaxBlock().toMillis()
                    )
            );

            if (blockMs == null || blockMs <= 0) {
                return BlockStatus.notBlocked();
            }

            meterRegistry.counter(
                    "ecofy.auth.temporary_block"
            ).increment();

            log.warn(
                    "[RedisBruteForceProtectionAdapter] - [registerFailure] -> Bloqueio temporário aplicado durationMs={}",
                    blockMs
            );

            return BlockStatus.blockedFor(
                    Duration.ofMillis(blockMs)
            );
        } catch (RuntimeException ex) {
            return failOpen("registerFailure", ex);
        }
    }

    // Remove as falhas e libera o bloqueio associado à identidade.
    @Override
    public void reset(String key) {
        try {
            redisTemplate.delete(
                    List.of(
                            FAIL_PREFIX + key,
                            BLOCK_PREFIX + key
                    )
            );
        } catch (RuntimeException ex) {
            failOpen("reset", ex);
        }
    }

    // Registra a indisponibilidade do Redis e mantém o fluxo sem bloqueio.
    private BlockStatus failOpen(
            String operation,
            RuntimeException ex
    ) {
        meterRegistry.counter(
                "ecofy.auth.brute_force.error",
                "operation",
                operation
        ).increment();

        log.error(
                "[RedisBruteForceProtectionAdapter] - [{}] -> Redis indisponível; seguindo sem bloqueio (fail-open) error={}",
                operation,
                ex.getMessage()
        );

        return BlockStatus.notBlocked();
    }
}

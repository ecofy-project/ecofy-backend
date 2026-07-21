package br.com.ecofy.auth.adapters.out.ratelimit;

import br.com.ecofy.auth.core.domain.ratelimit.RateLimitDecision;
import br.com.ecofy.auth.core.domain.ratelimit.RateLimitPolicy;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários do adaptador Redis de limitação de requisições")
class RedisRateLimiterAdapterTest {

    private static final String KEY = "usuario@ecofy.com";
    private static final String REDIS_KEY =
            "ecofy:auth:rl:" + KEY;
    private static final String POLICY_NAME = "login";
    private static final long NOW = 1_753_024_800_000L;
    private static final int LIMIT = 5;
    private static final Duration WINDOW =
            Duration.ofMinutes(1);

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private Clock clock;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter counter;

    @Test
    @DisplayName("Deve permitir a requisição quando o Redis retornar nulo")
    void tryConsume_resultadoNulo_devePermitirRequisicao() {
        // Arrange
        RateLimitPolicy policy = createPolicy();

        when(clock.millis()).thenReturn(NOW);
        when(executeRateLimitScript()).thenReturn(null);

        RedisRateLimiterAdapter adapter = createAdapter();

        // Act
        RateLimitDecision result = adapter.tryConsume(KEY, policy);

        // Assert
        assertEquals(RateLimitDecision.allow(), result);

        verifyScriptExecution(KEY, LIMIT, WINDOW);
        verifyNoInteractions(meterRegistry);
    }

    @Test
    @DisplayName("Deve permitir a requisição quando o Redis retornar valor negativo")
    void tryConsume_resultadoNegativo_devePermitirRequisicao() {
        // Arrange
        RateLimitPolicy policy = createPolicy();

        when(clock.millis()).thenReturn(NOW);
        when(executeRateLimitScript()).thenReturn(-1L);

        RedisRateLimiterAdapter adapter = createAdapter();

        // Act
        RateLimitDecision result = adapter.tryConsume(KEY, policy);

        // Assert
        assertEquals(RateLimitDecision.allow(), result);

        verifyScriptExecution(KEY, LIMIT, WINDOW);
        verifyNoInteractions(meterRegistry);
    }

    @Test
    @DisplayName("Deve negar a requisição sem espera quando o Redis retornar zero")
    void tryConsume_resultadoZero_deveNegarComDuracaoZero() {
        // Arrange
        RateLimitPolicy policy = createPolicy();

        when(clock.millis()).thenReturn(NOW);
        when(executeRateLimitScript()).thenReturn(0L);

        RedisRateLimiterAdapter adapter = createAdapter();

        // Act
        RateLimitDecision result = adapter.tryConsume(KEY, policy);

        // Assert
        assertEquals(
                RateLimitDecision.deny(Duration.ZERO),
                result
        );

        verifyScriptExecution(KEY, LIMIT, WINDOW);
        verifyNoInteractions(meterRegistry);
    }

    @Test
    @DisplayName("Deve negar a requisição pelo período informado pelo Redis")
    void tryConsume_resultadoPositivo_deveNegarComTempoDeEspera() {
        // Arrange
        RateLimitPolicy policy = createPolicy();
        long retryAfterMs = 30_000L;

        when(clock.millis()).thenReturn(NOW);
        when(executeRateLimitScript()).thenReturn(retryAfterMs);

        RedisRateLimiterAdapter adapter = createAdapter();

        // Act
        RateLimitDecision result = adapter.tryConsume(KEY, policy);

        // Assert
        assertEquals(
                RateLimitDecision.deny(
                        Duration.ofMillis(retryAfterMs)
                ),
                result
        );

        verifyScriptExecution(KEY, LIMIT, WINDOW);
        verifyNoInteractions(meterRegistry);
    }

    @Test
    @DisplayName("Deve executar o script com a chave, janela, limite e identificador corretos")
    void tryConsume_dadosValidos_deveExecutarScriptComArgumentosCorretos() {
        // Arrange
        RateLimitPolicy policy = createPolicy();

        when(clock.millis()).thenReturn(NOW);
        when(executeRateLimitScript()).thenReturn(-1L);

        RedisRateLimiterAdapter adapter = createAdapter();
        ArgumentCaptor<RedisScript<Long>> scriptCaptor =
                redisScriptCaptor();
        ArgumentCaptor<String> memberCaptor =
                ArgumentCaptor.forClass(String.class);

        // Act
        RateLimitDecision result = adapter.tryConsume(KEY, policy);

        // Assert
        verify(redisTemplate).execute(
                scriptCaptor.capture(),
                eq(List.of(REDIS_KEY)),
                eq(Long.toString(NOW)),
                eq(Long.toString(WINDOW.toMillis())),
                eq(Integer.toString(LIMIT)),
                memberCaptor.capture()
        );

        String member = memberCaptor.getValue();
        String uuidValue = member.substring(
                Long.toString(NOW).length() + 1
        );

        assertAll(
                () -> assertEquals(
                        RateLimitDecision.allow(),
                        result
                ),
                () -> assertNotNull(scriptCaptor.getValue()),
                () -> assertEquals(
                        Long.class,
                        scriptCaptor.getValue().getResultType()
                ),
                () -> assertTrue(
                        member.startsWith(NOW + "-")
                ),
                () -> assertNotNull(
                        UUID.fromString(uuidValue)
                )
        );
    }

    @Test
    @DisplayName("Deve gerar identificadores diferentes para consumos consecutivos")
    void tryConsume_chamadasConsecutivas_deveGerarMembrosDiferentes() {
        // Arrange
        RateLimitPolicy policy = createPolicy();

        when(clock.millis()).thenReturn(NOW);
        when(executeRateLimitScript()).thenReturn(-1L);

        RedisRateLimiterAdapter adapter = createAdapter();
        ArgumentCaptor<String> memberCaptor =
                ArgumentCaptor.forClass(String.class);

        // Act
        adapter.tryConsume(KEY, policy);
        adapter.tryConsume(KEY, policy);

        // Assert
        verify(redisTemplate, times(2)).execute(
                any(RedisScript.class),
                eq(List.of(REDIS_KEY)),
                eq(Long.toString(NOW)),
                eq(Long.toString(WINDOW.toMillis())),
                eq(Integer.toString(LIMIT)),
                memberCaptor.capture()
        );

        List<String> members = memberCaptor.getAllValues();

        assertAll(
                () -> assertEquals(2, members.size()),
                () -> assertTrue(
                        members.get(0).startsWith(NOW + "-")
                ),
                () -> assertTrue(
                        members.get(1).startsWith(NOW + "-")
                ),
                () -> assertNotEquals(
                        members.get(0),
                        members.get(1)
                )
        );
    }

    @Test
    @DisplayName("Deve concatenar chave nula na chave utilizada pelo Redis")
    void tryConsume_chaveNula_deveExecutarScriptComSufixoNull() {
        // Arrange
        RateLimitPolicy policy = createPolicy();
        String expectedRedisKey = "ecofy:auth:rl:null";

        when(clock.millis()).thenReturn(NOW);
        when(executeRateLimitScript()).thenReturn(-1L);

        RedisRateLimiterAdapter adapter = createAdapter();

        // Act
        RateLimitDecision result = adapter.tryConsume(null, policy);

        // Assert
        assertEquals(RateLimitDecision.allow(), result);

        verifyScriptExecution(null, LIMIT, WINDOW);

        verify(redisTemplate).execute(
                any(RedisScript.class),
                eq(List.of(expectedRedisKey)),
                eq(Long.toString(NOW)),
                eq(Long.toString(WINDOW.toMillis())),
                eq(Integer.toString(LIMIT)),
                any(String.class)
        );
    }

    @Test
    @DisplayName("Deve executar o script com limite zero quando informado pela política")
    void tryConsume_limiteZero_deveEnviarZeroAoRedis() {
        // Arrange
        RateLimitPolicy policy = mock(RateLimitPolicy.class);

        when(policy.limit()).thenReturn(0);
        when(policy.window()).thenReturn(WINDOW);
        when(clock.millis()).thenReturn(NOW);
        when(executeRateLimitScript()).thenReturn(-1L);

        RedisRateLimiterAdapter adapter = createAdapter();

        // Act
        RateLimitDecision result = adapter.tryConsume(KEY, policy);

        // Assert
        assertEquals(RateLimitDecision.allow(), result);

        verifyScriptExecution(KEY, 0, WINDOW);
    }

    @Test
    @DisplayName("Deve registrar métrica e permitir a requisição quando o Redis estiver indisponível")
    void tryConsume_redisLancaExcecao_deveRegistrarErroEPermitirRequisicao() {
        // Arrange
        RateLimitPolicy policy = createPolicy();
        RuntimeException redisException =
                new RuntimeException("Redis indisponível");

        when(policy.name()).thenReturn(POLICY_NAME);
        when(clock.millis()).thenReturn(NOW);
        when(executeRateLimitScript())
                .thenThrow(redisException);
        when(
                meterRegistry.counter(
                        "ecofy.auth.rate_limit.error",
                        "policy",
                        POLICY_NAME
                )
        ).thenReturn(counter);

        RedisRateLimiterAdapter adapter = createAdapter();

        // Act
        RateLimitDecision result = adapter.tryConsume(KEY, policy);

        // Assert
        assertAll(
                () -> assertEquals(
                        RateLimitDecision.allow(),
                        result
                ),
                () -> verify(meterRegistry).counter(
                        "ecofy.auth.rate_limit.error",
                        "policy",
                        POLICY_NAME
                ),
                () -> verify(counter).increment()
        );
    }

    @Test
    @DisplayName("Deve propagar a exceção quando o relógio falhar antes da consulta ao Redis")
    void tryConsume_clockLancaExcecao_devePropagarExcecao() {
        // Arrange
        RateLimitPolicy policy = mock(RateLimitPolicy.class);
        RuntimeException clockException =
                new RuntimeException("Relógio indisponível");

        when(clock.millis()).thenThrow(clockException);

        RedisRateLimiterAdapter adapter = createAdapter();

        // Act
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> adapter.tryConsume(KEY, policy)
        );

        // Assert
        assertAll(
                () -> assertSame(clockException, exception),
                () -> verifyNoInteractions(
                        redisTemplate,
                        meterRegistry
                )
        );
    }

    @Test
    @DisplayName("Deve rejeitar política nula antes de executar o script Redis")
    void tryConsume_politicaNula_deveLancarNullPointerException() {
        // Arrange
        when(clock.millis()).thenReturn(NOW);

        RedisRateLimiterAdapter adapter = createAdapter();

        // Act
        assertThrows(
                NullPointerException.class,
                () -> adapter.tryConsume(KEY, null)
        );

        // Assert
        verify(clock).millis();
        verifyNoInteractions(
                redisTemplate,
                meterRegistry
        );
    }

    @Test
    @DisplayName("Deve propagar exceção quando a janela da política for nula")
    void tryConsume_janelaNula_deveLancarNullPointerException() {
        // Arrange
        RateLimitPolicy policy = mock(RateLimitPolicy.class);

        when(policy.window()).thenReturn(null);
        when(clock.millis()).thenReturn(NOW);

        RedisRateLimiterAdapter adapter = createAdapter();

        // Act
        assertThrows(
                NullPointerException.class,
                () -> adapter.tryConsume(KEY, policy)
        );

        // Assert
        verify(clock).millis();
        verify(policy).window();
        verifyNoInteractions(
                redisTemplate,
                meterRegistry
        );
    }

    private RedisRateLimiterAdapter createAdapter() {
        return new RedisRateLimiterAdapter(
                redisTemplate,
                clock,
                meterRegistry
        );
    }

    private RateLimitPolicy createPolicy() {
        RateLimitPolicy policy = mock(RateLimitPolicy.class);

        when(policy.limit()).thenReturn(LIMIT);
        when(policy.window()).thenReturn(WINDOW);

        return policy;
    }

    @SuppressWarnings("unchecked")
    private Long executeRateLimitScript() {
        return (Long) redisTemplate.execute(
                any(RedisScript.class),
                anyList(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<RedisScript<Long>> redisScriptCaptor() {
        return ArgumentCaptor.forClass(
                (Class<RedisScript<Long>>) (Class<?>) RedisScript.class
        );
    }

    @SuppressWarnings("unchecked")
    private void verifyScriptExecution(
            String key,
            int limit,
            Duration window
    ) {
        String expectedRedisKey = "ecofy:auth:rl:" + key;
        String expectedNow = Long.toString(NOW);
        String expectedWindow = Long.toString(window.toMillis());
        String expectedLimit = Integer.toString(limit);

        verify(redisTemplate).execute(
                any(RedisScript.class),
                eq(List.of(expectedRedisKey)),
                eq(expectedNow),
                eq(expectedWindow),
                eq(expectedLimit),
                any(String.class)
        );
    }
}

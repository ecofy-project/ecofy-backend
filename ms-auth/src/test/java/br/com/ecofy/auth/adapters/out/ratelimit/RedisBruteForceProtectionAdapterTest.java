package br.com.ecofy.auth.adapters.out.ratelimit;

import br.com.ecofy.auth.config.BruteForceProperties;
import br.com.ecofy.auth.core.domain.bruteforce.BlockStatus;
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

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes unitários do adaptador Redis de proteção contra força bruta")
class RedisBruteForceProtectionAdapterTest {

    private static final String KEY = "usuario@ecofy.com";
    private static final String FAIL_KEY =
            "ecofy:auth:bf:fail:" + KEY;
    private static final String BLOCK_KEY =
            "ecofy:auth:bf:block:" + KEY;
    private static final String LEVEL_KEY =
            "ecofy:auth:bf:level:" + KEY;

    private static final int THRESHOLD = 5;
    private static final Duration OBSERVATION_WINDOW =
            Duration.ofMinutes(10);
    private static final Duration INITIAL_BLOCK =
            Duration.ofMinutes(1);
    private static final double MULTIPLIER = 2.0;
    private static final Duration MAX_BLOCK =
            Duration.ofHours(1);

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private BruteForceProperties properties;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter counter;

    @Test
    @DisplayName("Deve retornar não bloqueado quando o Redis não possuir TTL")
    void status_ttlNulo_deveRetornarNaoBloqueado() {
        // Arrange
        when(
                redisTemplate.getExpire(
                        BLOCK_KEY,
                        TimeUnit.MILLISECONDS
                )
        ).thenReturn(null);

        RedisBruteForceProtectionAdapter adapter = createAdapter();

        // Act
        BlockStatus result = adapter.status(KEY);

        // Assert
        assertEquals(BlockStatus.notBlocked(), result);

        verify(redisTemplate).getExpire(
                BLOCK_KEY,
                TimeUnit.MILLISECONDS
        );
        verifyNoInteractions(meterRegistry);
    }

    @Test
    @DisplayName("Deve retornar não bloqueado quando o TTL for negativo")
    void status_ttlNegativo_deveRetornarNaoBloqueado() {
        // Arrange
        when(
                redisTemplate.getExpire(
                        BLOCK_KEY,
                        TimeUnit.MILLISECONDS
                )
        ).thenReturn(-1L);

        RedisBruteForceProtectionAdapter adapter = createAdapter();

        // Act
        BlockStatus result = adapter.status(KEY);

        // Assert
        assertEquals(BlockStatus.notBlocked(), result);

        verify(redisTemplate).getExpire(
                BLOCK_KEY,
                TimeUnit.MILLISECONDS
        );
        verifyNoInteractions(meterRegistry);
    }

    @Test
    @DisplayName("Deve retornar bloqueado com duração zero quando o TTL for zero")
    void status_ttlZero_deveRetornarBloqueadoComDuracaoZero() {
        // Arrange
        when(
                redisTemplate.getExpire(
                        BLOCK_KEY,
                        TimeUnit.MILLISECONDS
                )
        ).thenReturn(0L);

        RedisBruteForceProtectionAdapter adapter = createAdapter();

        // Act
        BlockStatus result = adapter.status(KEY);

        // Assert
        assertEquals(
                BlockStatus.blockedFor(Duration.ZERO),
                result
        );

        verify(redisTemplate).getExpire(
                BLOCK_KEY,
                TimeUnit.MILLISECONDS
        );
    }

    @Test
    @DisplayName("Deve retornar bloqueado com o tempo restante informado pelo Redis")
    void status_ttlPositivo_deveRetornarBloqueadoComDuracaoRestante() {
        // Arrange
        long ttlMs = 30_000L;

        when(
                redisTemplate.getExpire(
                        BLOCK_KEY,
                        TimeUnit.MILLISECONDS
                )
        ).thenReturn(ttlMs);

        RedisBruteForceProtectionAdapter adapter = createAdapter();

        // Act
        BlockStatus result = adapter.status(KEY);

        // Assert
        assertEquals(
                BlockStatus.blockedFor(Duration.ofMillis(ttlMs)),
                result
        );

        verify(redisTemplate).getExpire(
                BLOCK_KEY,
                TimeUnit.MILLISECONDS
        );
    }

    @Test
    @DisplayName("Deve concatenar chave nula e consultar o status no Redis")
    void status_chaveNula_deveConsultarRedisComSufixoNull() {
        // Arrange
        String expectedKey = "ecofy:auth:bf:block:null";

        when(
                redisTemplate.getExpire(
                        expectedKey,
                        TimeUnit.MILLISECONDS
                )
        ).thenReturn(null);

        RedisBruteForceProtectionAdapter adapter = createAdapter();

        // Act
        BlockStatus result = adapter.status(null);

        // Assert
        assertEquals(BlockStatus.notBlocked(), result);

        verify(redisTemplate).getExpire(
                expectedKey,
                TimeUnit.MILLISECONDS
        );
    }

    @Test
    @DisplayName("Deve aplicar fail-open quando o Redis falhar ao consultar o status")
    void status_redisLancaExcecao_deveRegistrarErroERetornarNaoBloqueado() {
        // Arrange
        RuntimeException redisException =
                new RuntimeException("Redis indisponível");

        when(
                redisTemplate.getExpire(
                        BLOCK_KEY,
                        TimeUnit.MILLISECONDS
                )
        ).thenThrow(redisException);
        when(
                meterRegistry.counter(
                        "ecofy.auth.brute_force.error",
                        "operation",
                        "status"
                )
        ).thenReturn(counter);

        RedisBruteForceProtectionAdapter adapter = createAdapter();

        // Act
        BlockStatus result = adapter.status(KEY);

        // Assert
        assertAll(
                () -> assertEquals(
                        BlockStatus.notBlocked(),
                        result
                ),
                () -> verify(meterRegistry).counter(
                        "ecofy.auth.brute_force.error",
                        "operation",
                        "status"
                ),
                () -> verify(counter).increment()
        );
    }

    @Test
    @DisplayName("Deve retornar não bloqueado quando o script não retornar duração")
    void registerFailure_resultadoNulo_deveRetornarNaoBloqueado() {
        // Arrange
        configureProperties();

        when(executeRegisterFailureScript()).thenReturn(null);

        RedisBruteForceProtectionAdapter adapter = createAdapter();

        // Act
        BlockStatus result = adapter.registerFailure(KEY);

        // Assert
        assertEquals(BlockStatus.notBlocked(), result);

        verifyRegisterFailureExecution(KEY);
        verifyNoInteractions(meterRegistry);
    }

    @Test
    @DisplayName("Deve retornar não bloqueado quando o script retornar zero")
    void registerFailure_resultadoZero_deveRetornarNaoBloqueado() {
        // Arrange
        configureProperties();

        when(executeRegisterFailureScript()).thenReturn(0L);

        RedisBruteForceProtectionAdapter adapter = createAdapter();

        // Act
        BlockStatus result = adapter.registerFailure(KEY);

        // Assert
        assertEquals(BlockStatus.notBlocked(), result);

        verifyRegisterFailureExecution(KEY);
        verifyNoInteractions(meterRegistry);
    }

    @Test
    @DisplayName("Deve retornar não bloqueado quando o script retornar valor negativo")
    void registerFailure_resultadoNegativo_deveRetornarNaoBloqueado() {
        // Arrange
        configureProperties();

        when(executeRegisterFailureScript()).thenReturn(-1L);

        RedisBruteForceProtectionAdapter adapter = createAdapter();

        // Act
        BlockStatus result = adapter.registerFailure(KEY);

        // Assert
        assertEquals(BlockStatus.notBlocked(), result);

        verifyRegisterFailureExecution(KEY);
        verifyNoInteractions(meterRegistry);
    }

    @Test
    @DisplayName("Deve registrar métrica e retornar bloqueado quando o script aplicar bloqueio")
    void registerFailure_bloqueioAplicado_deveIncrementarMetricaERetornarBloqueado() {
        // Arrange
        long blockMs = 120_000L;
        configureProperties();

        when(executeRegisterFailureScript()).thenReturn(blockMs);
        when(
                meterRegistry.counter(
                        "ecofy.auth.temporary_block"
                )
        ).thenReturn(counter);

        RedisBruteForceProtectionAdapter adapter = createAdapter();

        // Act
        BlockStatus result = adapter.registerFailure(KEY);

        // Assert
        assertAll(
                () -> assertEquals(
                        BlockStatus.blockedFor(
                                Duration.ofMillis(blockMs)
                        ),
                        result
                ),
                () -> verify(meterRegistry).counter(
                        "ecofy.auth.temporary_block"
                ),
                () -> verify(counter).increment()
        );

        verifyRegisterFailureExecution(KEY);
    }

    @Test
    @DisplayName("Deve utilizar as propriedades configuradas como argumentos do script Redis")
    void registerFailure_propriedadesConfiguradas_deveExecutarScriptComArgumentosCorretos() {
        // Arrange
        configureProperties();

        when(executeRegisterFailureScript()).thenReturn(0L);

        RedisBruteForceProtectionAdapter adapter = createAdapter();

        // Act
        adapter.registerFailure(KEY);

        // Assert
        ArgumentCaptor<RedisScript<Long>> scriptCaptor =
                redisScriptCaptor();

        verify(redisTemplate).execute(
                scriptCaptor.capture(),
                eq(List.of(FAIL_KEY, BLOCK_KEY, LEVEL_KEY)),
                eq(Integer.toString(THRESHOLD)),
                eq(Long.toString(OBSERVATION_WINDOW.toMillis())),
                eq(Long.toString(INITIAL_BLOCK.toMillis())),
                eq(Double.toString(MULTIPLIER)),
                eq(Long.toString(MAX_BLOCK.toMillis()))
        );

        assertAll(
                () -> assertNotNull(scriptCaptor.getValue()),
                () -> assertEquals(
                        Long.class,
                        scriptCaptor.getValue().getResultType()
                )
        );
    }

    @Test
    @DisplayName("Deve concatenar chave nula nas chaves utilizadas pelo script Redis")
    void registerFailure_chaveNula_deveExecutarScriptComSufixoNull() {
        // Arrange
        configureProperties();

        when(executeRegisterFailureScript()).thenReturn(0L);

        RedisBruteForceProtectionAdapter adapter = createAdapter();

        // Act
        BlockStatus result = adapter.registerFailure(null);

        // Assert
        assertEquals(BlockStatus.notBlocked(), result);

        verify(redisTemplate).execute(
                any(RedisScript.class),
                eq(
                        List.of(
                                "ecofy:auth:bf:fail:null",
                                "ecofy:auth:bf:block:null",
                                "ecofy:auth:bf:level:null"
                        )
                ),
                eq(Integer.toString(THRESHOLD)),
                eq(Long.toString(OBSERVATION_WINDOW.toMillis())),
                eq(Long.toString(INITIAL_BLOCK.toMillis())),
                eq(Double.toString(MULTIPLIER)),
                eq(Long.toString(MAX_BLOCK.toMillis()))
        );
    }

    @Test
    @DisplayName("Deve aplicar fail-open quando o Redis falhar ao registrar a tentativa")
    void registerFailure_redisLancaExcecao_deveRegistrarErroERetornarNaoBloqueado() {
        // Arrange
        configureProperties();
        RuntimeException redisException =
                new RuntimeException("Falha ao executar script");

        when(executeRegisterFailureScript())
                .thenThrow(redisException);
        when(
                meterRegistry.counter(
                        "ecofy.auth.brute_force.error",
                        "operation",
                        "registerFailure"
                )
        ).thenReturn(counter);

        RedisBruteForceProtectionAdapter adapter = createAdapter();

        // Act
        BlockStatus result = adapter.registerFailure(KEY);

        // Assert
        assertAll(
                () -> assertEquals(
                        BlockStatus.notBlocked(),
                        result
                ),
                () -> verify(meterRegistry).counter(
                        "ecofy.auth.brute_force.error",
                        "operation",
                        "registerFailure"
                ),
                () -> verify(counter).increment()
        );
    }

    @Test
    @DisplayName("Deve remover as chaves de falha e bloqueio ao redefinir a proteção")
    void reset_chaveValida_deveRemoverFalhasEBloqueio() {
        // Arrange
        RedisBruteForceProtectionAdapter adapter = createAdapter();

        // Act
        adapter.reset(KEY);

        // Assert
        verify(redisTemplate).delete(
                List.of(FAIL_KEY, BLOCK_KEY)
        );
        verifyNoInteractions(meterRegistry);
    }

    @Test
    @DisplayName("Deve concatenar chave nula ao redefinir a proteção")
    void reset_chaveNula_deveRemoverChavesComSufixoNull() {
        // Arrange
        RedisBruteForceProtectionAdapter adapter = createAdapter();

        // Act
        adapter.reset(null);

        // Assert
        verify(redisTemplate).delete(
                List.of(
                        "ecofy:auth:bf:fail:null",
                        "ecofy:auth:bf:block:null"
                )
        );
    }

    @Test
    @DisplayName("Deve aplicar fail-open quando o Redis falhar ao redefinir a proteção")
    void reset_redisLancaExcecao_deveRegistrarErroSemPropagarExcecao() {
        // Arrange
        RuntimeException redisException =
                new RuntimeException("Falha ao remover chaves");

        when(
                redisTemplate.delete(
                        List.of(FAIL_KEY, BLOCK_KEY)
                )
        ).thenThrow(redisException);
        when(
                meterRegistry.counter(
                        "ecofy.auth.brute_force.error",
                        "operation",
                        "reset"
                )
        ).thenReturn(counter);

        RedisBruteForceProtectionAdapter adapter = createAdapter();

        // Act
        adapter.reset(KEY);

        // Assert
        assertAll(
                () -> verify(meterRegistry).counter(
                        "ecofy.auth.brute_force.error",
                        "operation",
                        "reset"
                ),
                () -> verify(counter).increment()
        );
    }

    private RedisBruteForceProtectionAdapter createAdapter() {
        return new RedisBruteForceProtectionAdapter(
                redisTemplate,
                properties,
                meterRegistry
        );
    }

    private void configureProperties() {
        when(properties.getThreshold()).thenReturn(THRESHOLD);
        when(properties.getObservationWindow())
                .thenReturn(OBSERVATION_WINDOW);
        when(properties.getInitialBlock())
                .thenReturn(INITIAL_BLOCK);
        when(properties.getMultiplier())
                .thenReturn(MULTIPLIER);
        when(properties.getMaxBlock()).thenReturn(MAX_BLOCK);
    }

    @SuppressWarnings("unchecked")
    private Long executeRegisterFailureScript() {
        return (Long) redisTemplate.execute(
                any(RedisScript.class),
                anyList(),
                any(),
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
    private void verifyRegisterFailureExecution(String key) {
        verify(redisTemplate).execute(
                any(RedisScript.class),
                eq(
                        List.of(
                                "ecofy:auth:bf:fail:" + key,
                                "ecofy:auth:bf:block:" + key,
                                "ecofy:auth:bf:level:" + key
                        )
                ),
                eq(Integer.toString(THRESHOLD)),
                eq(Long.toString(OBSERVATION_WINDOW.toMillis())),
                eq(Long.toString(INITIAL_BLOCK.toMillis())),
                eq(Double.toString(MULTIPLIER)),
                eq(Long.toString(MAX_BLOCK.toMillis()))
        );
    }
}

package br.com.ecofy.ms_users.core.port.out;

/**
 * Resultado do registro de uma chave de idempotência para uma operação.
 *
 * <ul>
 *   <li>{@link #REGISTERED}: primeira vez — a chave foi registrada e a operação deve ser aplicada.</li>
 *   <li>{@link #DUPLICATE}: mesma operação + mesma chave + mesmo request hash — retry legítimo;
 *       a operação NÃO deve ser reaplicada (deve-se retornar o estado atual).</li>
 *   <li>{@link #CONFLICT}: mesma operação + mesma chave + request hash DIFERENTE — conflito real (409).</li>
 * </ul>
 */
public enum IdempotencyOutcome {
    REGISTERED,
    DUPLICATE,
    CONFLICT
}

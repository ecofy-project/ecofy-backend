package br.com.ecofy.auth.core.port.out;

import br.com.ecofy.auth.core.domain.bruteforce.BlockStatus;

// Protege contra brute force contando falhas de autenticação e aplicando bloqueio progressivo.
public interface BruteForceProtectionPort {

    // Recupera o estado de bloqueio da chave sem alterar contadores.
    BlockStatus status(String key);

    // Registra uma falha de autenticação e aplica bloqueio progressivo ao atingir o limiar.
    BlockStatus registerFailure(String key);

    // Limpa os contadores após autenticação legítima.
    void reset(String key);
}

package br.com.ecofy.auth.core.port.out;

import br.com.ecofy.auth.core.domain.keys.ActiveSigningKey;
import br.com.ecofy.auth.core.domain.keys.VerificationKey;
import java.util.List;

// Fornece as chaves de assinatura, abstraindo a origem (arquivo, Secret Manager ou KMS).
public interface SigningKeyProviderPort {

    // Recupera a chave ativa usada para assinar tokens novos.
    ActiveSigningKey activeKey();

    // Recupera as chaves públicas aceitas para verificação, incluindo as aposentadas na janela.
    List<VerificationKey> verificationKeys();
}

package br.com.ecofy.auth.adapters.out.persistence;

import br.com.ecofy.auth.adapters.out.persistence.mapper.PersistenceMapper;
import br.com.ecofy.auth.adapters.out.persistence.repository.ClientApplicationRepository;
import br.com.ecofy.auth.core.domain.ClientApplication;
import br.com.ecofy.auth.core.port.out.LoadClientApplicationByClientIdPort;
import br.com.ecofy.auth.core.port.out.SaveClientApplicationPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

@Component
@Slf4j
public class ClientApplicationJpaAdapter implements SaveClientApplicationPort, LoadClientApplicationByClientIdPort {

    private final ClientApplicationRepository repository;

    // Injeta o repositório JPA e garante que ele não seja nulo para operações de persistência/consulta.
    public ClientApplicationJpaAdapter(ClientApplicationRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    // Salva a aplicação cliente (cria/atualiza), garantindo timestamps e retornando o domínio mapeado.
    @Override
    @Transactional
    public ClientApplication save(ClientApplication clientApplication) {
        Objects.requireNonNull(clientApplication, "clientApplication must not be null");

        log.debug(
                "[ClientApplicationJpaAdapter] - [save] -> Salvando clientApplication clientId={} name={}",
                clientApplication.clientId(),
                clientApplication.name()
        );

        var now = Instant.now();

        var entity = PersistenceMapper.toEntity(clientApplication);

        // Define createdAt apenas na criação do registro.
        if (entity.getCreatedAt() == null) {
            log.debug("[ClientApplicationJpaAdapter] - [save] -> Criando novo registro clientId={}",
                    entity.getClientId());
            entity.setCreatedAt(now);
        }

        // Atualiza updatedAt em toda gravação.
        entity.setUpdatedAt(now);

        var saved = repository.save(entity);

        log.debug(
                "[ClientApplicationJpaAdapter] - [save] -> Registro persistido com sucesso clientId={} updatedAt={}",
                saved.getClientId(),
                saved.getUpdatedAt()
        );

        return PersistenceMapper.toDomain(saved);
    }

    // Busca uma aplicação cliente pelo clientId e retorna Optional vazio quando não encontrada.
    @Override
    @Transactional(readOnly = true)
    public Optional<ClientApplication> loadByClientId(String clientId) {
        Objects.requireNonNull(clientId, "clientId must not be null");

        log.debug(
                "[ClientApplicationJpaAdapter] - [loadByClientId] -> Buscando aplicação por clientId={}",
                clientId
        );

        return repository.findByClientId(clientId)
                .map(entity -> {
                            log.debug(
                                    "[ClientApplicationJpaAdapter] - [loadByClientId] -> Aplicação encontrada clientId={}",
                                    entity.getClientId()
                            );
                            return PersistenceMapper.toDomain(entity);
                        }
                );

    }

}
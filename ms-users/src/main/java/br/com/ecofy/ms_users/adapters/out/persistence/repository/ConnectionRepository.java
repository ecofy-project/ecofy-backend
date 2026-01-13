package br.com.ecofy.ms_users.adapters.out.persistence.repository;

import br.com.ecofy.ms_users.adapters.out.persistence.entity.ConnectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConnectionRepository extends JpaRepository<ConnectionEntity, UUID> {
    List<ConnectionEntity> findByUserId(UUID userId);
}

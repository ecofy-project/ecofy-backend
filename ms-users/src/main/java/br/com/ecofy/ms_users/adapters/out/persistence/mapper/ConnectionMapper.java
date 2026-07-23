package br.com.ecofy.ms_users.adapters.out.persistence.mapper;

import br.com.ecofy.ms_users.adapters.out.persistence.entity.ConnectionEntity;
import br.com.ecofy.ms_users.core.domain.Connection;
import br.com.ecofy.ms_users.core.domain.valueobject.UserId;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

// Converte conexões entre os modelos de domínio e persistência.
public final class ConnectionMapper {

    private static final TypeReference<Map<String, Object>> MAP_REF =
            new TypeReference<>() {
            };
    private static final String EMPTY_JSON_OBJECT = "{}";

    private final ObjectMapper objectMapper;

    public ConnectionMapper(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper must not be null"
        );
    }

    // Converte a entidade persistida em uma conexão de domínio.
    public Connection toDomain(ConnectionEntity entity) {
        Objects.requireNonNull(entity, "entity must not be null");

        return Connection.builder()
                .id(entity.getId())
                .userId(UserId.of(entity.getUserId()))
                .type(entity.getType())
                .provider(entity.getProvider())
                .metadata(readJsonToMap(entity.getMetadataJson()))
                .createdAt(entity.getCreatedAt())
                .build();
    }

    // Converte a conexão de domínio em uma entidade persistível.
    public ConnectionEntity toEntity(Connection domain) {
        Objects.requireNonNull(domain, "domain must not be null");
        Objects.requireNonNull(
                domain.getUserId(),
                "domain.userId must not be null"
        );

        var entity = new ConnectionEntity();
        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId().value());
        entity.setType(domain.getType());
        entity.setProvider(domain.getProvider());
        entity.setMetadataJson(writeMapToJson(domain.getMetadata()));
        entity.setCreatedAt(domain.getCreatedAt());

        return entity;
    }

    // Converte metadados JSON com fallback para um mapa vazio.
    private Map<String, Object> readJsonToMap(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }

        try {
            Map<String, Object> parsed =
                    objectMapper.readValue(json, MAP_REF);

            return parsed == null
                    ? Collections.emptyMap()
                    : parsed;
        } catch (Exception ex) {
            return Collections.emptyMap();
        }
    }

    // Converte metadados em JSON com fallback para um objeto vazio.
    private String writeMapToJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return EMPTY_JSON_OBJECT;
        }

        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception ex) {
            return EMPTY_JSON_OBJECT;
        }
    }
}

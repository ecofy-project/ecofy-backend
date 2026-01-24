package br.com.ecofy.ms_users.adapters.out.persistence.mapper;

import br.com.ecofy.ms_users.adapters.out.persistence.entity.ConnectionEntity;
import br.com.ecofy.ms_users.core.domain.Connection;
import br.com.ecofy.ms_users.core.domain.valueobject.UserId;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public final class ConnectionMapper {

    private static final TypeReference<Map<String, Object>> MAP_REF = new TypeReference<>() {};
    private static final String EMPTY_JSON_OBJECT = "{}";

    private final ObjectMapper objectMapper;

    // Inicializa o mapper com o ObjectMapper usado para serializar/deserializar metadata (JSON <-> Map).
    public ConnectionMapper(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    // Converte a ConnectionEntity (persistência) para Connection (domínio), incluindo metadata JSON convertida para Map.
    public Connection toDomain(ConnectionEntity e) {
        Objects.requireNonNull(e, "entity must not be null");

        return Connection.builder()
                .id(e.getId())
                .userId(UserId.of(e.getUserId()))
                .type(e.getType())
                .provider(e.getProvider())
                .metadata(readJsonToMap(e.getMetadataJson()))
                .createdAt(e.getCreatedAt())
                .build();
    }

    // Converte a Connection (domínio) para ConnectionEntity (persistência), serializando metadata Map para JSON.
    public ConnectionEntity toEntity(Connection d) {
        Objects.requireNonNull(d, "domain must not be null");
        Objects.requireNonNull(d.getUserId(), "domain.userId must not be null");

        var e = new ConnectionEntity();
        e.setId(d.getId());
        e.setUserId(d.getUserId().value());
        e.setType(d.getType());
        e.setProvider(d.getProvider());
        e.setMetadataJson(writeMapToJson(d.getMetadata()));
        e.setCreatedAt(d.getCreatedAt());
        return e;
    }

    // Desserializa um JSON em Map<String, Object>, retornando Map vazio quando o JSON é ausente/ inválido.
    private Map<String, Object> readJsonToMap(String json) {
        if (json == null || json.isBlank()) return Collections.emptyMap();

        try {
            Map<String, Object> parsed = objectMapper.readValue(json, MAP_REF);
            return parsed == null ? Collections.emptyMap() : parsed;
        } catch (Exception ex) {
            // Política: ser tolerante a dado legado/corrompido e não quebrar carregamento.
            // Se preferir fail-fast, troque por RuntimeException com mensagem e cause.
            return Collections.emptyMap();
        }
    }

    // Serializa um Map<String, Object> em JSON, retornando "{}" quando o Map é nulo/vazio ou ocorre erro de serialização.
    private String writeMapToJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return EMPTY_JSON_OBJECT;

        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception ex) {
            // Política: não quebrar persistência por metadata inválida.
            return EMPTY_JSON_OBJECT;
        }
    }

}

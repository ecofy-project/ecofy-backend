package br.com.ecofy.auth.adapters.out.persistence.entity;

import br.com.ecofy.auth.core.domain.enums.ClientType;
import br.com.ecofy.auth.core.domain.enums.GrantType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "auth_client_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientApplicationEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "client_id", unique = true, nullable = false, length = 100)
    private String clientId;

    @Column(name = "client_secret_hash", length = 255)
    private String clientSecretHash;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "client_type", nullable = false, length = 32)
    private ClientType clientType;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "auth_client_grants",
            joinColumns = @JoinColumn(name = "client_id", referencedColumnName = "client_id")
    )
    @Column(name = "grant_type", nullable = false, columnDefinition = "grant_type")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private Set<GrantType> grantTypes = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "auth_client_redirect_uris",
            joinColumns = @JoinColumn(
                    name = "client_id",
                    referencedColumnName = "client_id"
            )
    )
    @Column(name = "redirect_uri", length = 512)
    private Set<String> redirectUris;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "auth_client_scopes",
            joinColumns = @JoinColumn(
                    name = "client_id",
                    referencedColumnName = "client_id"
            )
    )
    @Column(name = "scope", length = 64)
    private Set<String> scopes;

    @Column(name = "first_party", nullable = false)
    private boolean firstParty;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

}

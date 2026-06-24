package br.com.ecofy.auth.core.domain;

import br.com.ecofy.auth.core.domain.enums.ClientType;
import br.com.ecofy.auth.core.domain.enums.GrantType;

import java.time.Instant;
import java.util.*;

// Agregado que representa um client OAuth2/OIDC registrado no ms-auth.
public class ClientApplication {

    // Identificador interno (UUID string). Nunca exposto a clientes.
    private final String id;

    // Identificador público usado como client_id no protocolo OAuth2/OIDC.
    private final String clientId;

    // Hash do client_secret (nunca armazenar o segredo em texto puro).
    private String clientSecretHash;

    private final String name;
    private final ClientType clientType;
    private final Set<GrantType> grantTypes;
    private final Set<String> redirectUris;
    private final Set<String> scopes;
    private final boolean firstParty;
    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;


    // Reconstrói o agregado a partir de dados persistidos, validando invariantes (tipo do client, segredo quando necessário e normalização de coleções).
    public ClientApplication(String id,
                             String clientId,
                             String clientSecretHash,
                             String name,
                             ClientType clientType,
                             Set<GrantType> grantTypes,
                             Set<String> redirectUris,
                             Set<String> scopes,
                             boolean firstParty,
                             boolean active,
                             Instant createdAt,
                             Instant updatedAt) {

        this.id = Objects.requireNonNull(id, "id must not be null");
        this.clientId = Objects.requireNonNull(clientId, "clientId must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.clientType = Objects.requireNonNull(clientType, "clientType must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");

        // Valida a obrigatoriedade de client_secret para clients CONFIDENTIAL para manter o modelo de segurança do domínio.
        if (clientType == ClientType.CONFIDENTIAL &&
                (clientSecretHash == null || clientSecretHash.isBlank())) {
            throw new IllegalArgumentException("clientSecretHash must be provided for CONFIDENTIAL clients");
        }
        this.clientSecretHash = clientSecretHash;

        // Inicializa os grant types assegurando coleções mutáveis internas e evitando NPE.
        this.grantTypes = grantTypes != null
                ? new HashSet<>(grantTypes)
                : new HashSet<>();

        // Normaliza redirect URIs removendo nulos/blank e aplicando trim.
        this.redirectUris = normalizeUriSet(redirectUris);

        // Normaliza scopes removendo nulos/blank e aplicando trim.
        this.scopes = normalizeScopeSet(scopes);

        this.firstParty = firstParty;
        this.active = active;
    }

    // Fábrica que cria um novo client com id interno, timestamps e status ativo por padrão.
    public static ClientApplication create(String name,
                                           ClientType clientType,
                                           Set<GrantType> grantTypes,
                                           Set<String> redirectUris,
                                           Set<String> scopes,
                                           boolean firstParty,
                                           String generatedClientId,
                                           String clientSecretHash) {

        Instant now = Instant.now();
        String internalId = UUID.randomUUID().toString();
        boolean active = true;

        return new ClientApplication(
                internalId,
                Objects.requireNonNull(generatedClientId, "generatedClientId must not be null"),
                clientSecretHash,
                name,
                clientType,
                grantTypes,
                redirectUris,
                scopes,
                firstParty,
                active,
                now,
                now
        );
    }

    // Retorna o identificador interno do client (uso exclusivo do backend/persistência).
    public String id() {
        return id;
    }

    // Retorna o client_id público utilizado nos fluxos OAuth2/OIDC.
    public String clientId() {
        return clientId;
    }

    // Retorna o hash do client_secret (quando aplicável) para validação/autenticação do client.
    public String clientSecretHash() {
        return clientSecretHash;
    }

    // Retorna o nome lógico do client para identificação administrativa.
    public String name() {
        return name;
    }

    // Retorna o tipo do client (PUBLIC, SPA, CONFIDENTIAL, M2M) para aplicar regras de segurança.
    public ClientType clientType() {
        return clientType;
    }

    // Retorna os grant types permitidos como visão somente-leitura.
    public Set<GrantType> grantTypes() {
        return Collections.unmodifiableSet(grantTypes);
    }

    // Retorna as redirect URIs cadastradas como visão somente-leitura.
    public Set<String> redirectUris() {
        return Collections.unmodifiableSet(redirectUris);
    }

    // Retorna as scopes cadastradas como visão somente-leitura.
    public Set<String> scopes() {
        return Collections.unmodifiableSet(scopes);
    }

    // Indica se o client é first-party (confiável/interno) para regras específicas de produto.
    public boolean isFirstParty() {
        return firstParty;
    }

    // Indica se o client está ativo e pode participar de novos fluxos OAuth2/OIDC.
    public boolean isActive() {
        return active;
    }

    // Retorna o timestamp de criação do registro do client.
    public Instant createdAt() {
        return createdAt;
    }

    // Retorna o timestamp da última atualização do agregado.
    public Instant updatedAt() {
        return updatedAt;
    }

    // Verifica se o client suporta um grant específico para autorização/autenticação.
    public boolean supportsGrant(GrantType grantType) {
        return grantTypes.contains(Objects.requireNonNull(grantType, "grantType must not be null"));
    }

    // Valida se a redirect_uri informada está autorizada, falhando fechado quando não há URIs configuradas.
    public boolean supportsRedirectUri(String redirectUri) {
        Objects.requireNonNull(redirectUri, "redirectUri must not be null");
        if (redirectUris.isEmpty()) {
            return false;
        }
        return redirectUris.contains(redirectUri.trim());
    }

    // Valida se a scope solicitada é permitida, falhando fechado quando não há scopes configuradas.
    public boolean supportsScope(String requestedScope) {
        Objects.requireNonNull(requestedScope, "requestedScope must not be null");
        if (scopes.isEmpty()) {
            return false;
        }
        return scopes.contains(requestedScope.trim());
    }

    // Verifica se todas as scopes solicitadas estão contidas nas scopes permitidas pelo client.
    public boolean supportsAllScopes(Set<String> requestedScopes) {
        if (requestedScopes == null || requestedScopes.isEmpty()) {
            return true; // nada solicitado, sempre ok
        }
        if (scopes.isEmpty()) {
            return false;
        }
        return requestedScopes.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .allMatch(scopes::contains);
    }

    // Rotaciona o segredo do client (apenas para CONFIDENTIAL) e atualiza o timestamp de modificação.
    public void rotateSecret(String newSecretHash) {
        if (clientType != ClientType.CONFIDENTIAL) {
            throw new IllegalStateException("Only CONFIDENTIAL clients can have a secret");
        }
        this.clientSecretHash = Objects.requireNonNull(newSecretHash, "newSecretHash must not be null");
        touch();
    }

    // Desativa o client para impedir novos fluxos OAuth2/OIDC enquanto preserva o histórico.
    public void deactivate() {
        if (!this.active) {
            return;
        }
        this.active = false;
        touch();
    }

    // Reativa o client para permitir novos fluxos OAuth2/OIDC após uma desativação.
    public void activate() {
        if (this.active) {
            return;
        }
        this.active = true;
        touch();
    }

    // Atualiza updatedAt para refletir alterações internas do agregado.
    private void touch() {
        this.updatedAt = Instant.now();
    }

    // Normaliza scopes removendo valores nulos/blank e aplicando trim para consistência.
    private static Set<String> normalizeScopeSet(Set<String> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            return new HashSet<>();
        }
        Set<String> normalized = new HashSet<>();
        for (String scope : scopes) {
            if (scope != null) {
                String trimmed = scope.trim();
                if (!trimmed.isEmpty()) {
                    normalized.add(trimmed);
                }
            }
        }
        return normalized;
    }

    // Normaliza redirect URIs removendo valores nulos/blank e aplicando trim para consistência.
    private static Set<String> normalizeUriSet(Set<String> uris) {
        if (uris == null || uris.isEmpty()) {
            return new HashSet<>();
        }
        Set<String> normalized = new HashSet<>();
        for (String uri : uris) {
            if (uri != null) {
                String trimmed = uri.trim();
                if (!trimmed.isEmpty()) {
                    normalized.add(trimmed);
                }
            }
        }
        return normalized;
    }
}
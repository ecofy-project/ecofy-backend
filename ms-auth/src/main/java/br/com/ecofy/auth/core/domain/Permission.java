package br.com.ecofy.auth.core.domain;

import java.util.Locale;
import java.util.Objects;

// Permissão granular do sistema (ex.: "transactions:read") com suporte a wildcard por nome e por domínio.
public final class Permission {

    private final String name;
    private final String description;
    private final String domain;

    // Constrói a permissão normalizando/validando nome e domínio e armazenando descrição opcional.
    public Permission(String name, String description, String domain) {
        this.name = normalizeName(name);
        this.description = description;
        this.domain = normalizeDomain(domain);
    }

    // Retorna o identificador da permissão (ex.: "transactions:read").
    public String name() {
        return name;
    }

    // Retorna a descrição humana da permissão (opcional).
    public String description() {
        return description;
    }

    // Retorna o domínio lógico da permissão (ex.: "auth", "billing" ou "*").
    public String domain() {
        return domain;
    }

    // Verifica se esta permissão concede/implica outra, considerando wildcards e compatibilidade de domínio.
    public boolean implies(Permission other) {
        Objects.requireNonNull(other, "other must not be null");

        // Global wildcard: "*" implica qualquer permissão
        if (isWildcard()) {
            return true;
        }

        // Se os domains forem específicos e diferentes, não implica
        if (!"*".equals(this.domain) &&
                !"*".equals(other.domain) &&
                !this.domain.equalsIgnoreCase(other.domain)) {
            return false;
        }

        // Mesmo nome => implica
        if (this.name.equals(other.name)) {
            return true;
        }

        // "transactions:*" implica "transactions:read", "transactions:write" etc.
        if (isDomainWildcardName()) {
            String prefix = this.name.substring(0, this.name.length() - 1); // remove o '*'
            return other.name.startsWith(prefix);
        }

        return false;
    }

    // Verifica implicação a partir de um nome bruto de permissão, preservando o domínio atual como referência.
    public boolean implies(String otherPermissionName) {
        return implies(new Permission(otherPermissionName, null, this.domain));
    }

    // Indica se esta permissão é o wildcard global "*" (concede tudo).
    public boolean isWildcard() {
        return "*".equals(this.name);
    }

    // Indica se esta permissão é um wildcard por prefixo no padrão "prefix:*".
    public boolean isDomainWildcardName() {
        return this.name.endsWith(":*") && this.name.length() > 2;
    }

    // Normaliza/valida o nome da permissão (não nulo, trim e não vazio).
    private String normalizeName(String rawName) {
        Objects.requireNonNull(rawName, "name must not be null");
        String trimmed = rawName.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        return trimmed;
    }

    // Normaliza o domínio (default "*", trim e lowercase) para comparação consistente.
    private String normalizeDomain(String rawDomain) {
        if (rawDomain == null || rawDomain.isBlank()) {
            return "*";
        }
        return rawDomain.trim().toLowerCase(Locale.ROOT);
    }

    // Compara permissões pela chave "name" (sem considerar domínio/descrição).
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Permission that)) return false;
        return name.equals(that.name);
    }

    // Gera hashCode consistente com equals usando apenas "name".
    @Override
    public int hashCode() {
        return name.hashCode();
    }

    // Retorna representação textual enxuta para logs/debug (sem expor descrição).
    @Override
    public String toString() {
        return "Permission{" +
                "name='" + name + '\'' +
                ", domain='" + domain + '\'' +
                '}';
    }
}
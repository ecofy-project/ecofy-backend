package br.com.ecofy.auth.core.domain;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

// Role (papel) de autorização do sistema. Um Role agrupa um conjunto de permissões ({@link Permission}).
public final class Role {

    // Nome do role (ex.: ROLE_ADMIN, ROLE_USER).
    private final String name;

    // Descrição opcional do role (uso administrativo/visual).
    private final String description;

    // Conjunto de permissões concedidas por este role.
    private final Set<Permission> permissions;

    public Role(String name, String description, Set<Permission> permissions) {
        this.name = normalizeName(name);
        this.description = description;
        this.permissions = permissions != null
                ? new HashSet<>(permissions)
                : new HashSet<>();
    }

    // Getters
    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public Set<Permission> permissions() {
        return Collections.unmodifiableSet(permissions);
    }

    // Regras de domínio

    // Verifica se o role contém uma permissão exatamente igual ao nome informado (não considera wildcards).
    public boolean hasExactPermission(String permissionName) {
        Objects.requireNonNull(permissionName, "permissionName must not be null");
        return permissions.stream()
                .anyMatch(p -> p.name().equals(permissionName));
    }

    // Verifica se o role concede uma permissão que implique a permissão solicitada (considera wildcards).
    public boolean hasPermission(String permissionName) {
        Objects.requireNonNull(permissionName, "permissionName must not be null");
        return implies(new Permission(permissionName, null, "*"));
    }

    // Verifica se o role implica a permissão informada, usando a regra de {@link Permission#implies(Permission)}.
    public boolean implies(Permission permission) {
        Objects.requireNonNull(permission, "permission must not be null");
        return permissions.stream().anyMatch(p -> p.implies(permission));
    }

    // Retorna uma nova instância de Role com a permissão adicionada (não altera o objeto atual).
    public Role withPermission(Permission permission) {
        Objects.requireNonNull(permission, "permission must not be null");
        Set<Permission> newPerms = new HashSet<>(this.permissions);
        newPerms.add(permission);
        return new Role(this.name, this.description, newPerms);
    }

    // Retorna uma nova instância de Role sem a permissão informada (não altera o objeto atual).
    public Role withoutPermission(Permission permission) {
        Objects.requireNonNull(permission, "permission must not be null");
        Set<Permission> newPerms = new HashSet<>(this.permissions);
        newPerms.remove(permission);
        return new Role(this.name, this.description, newPerms);
    }

    // Normaliza/valida o nome do role.
    private String normalizeName(String rawName) {
        Objects.requireNonNull(rawName, "name must not be null");
        String trimmed = rawName.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("name must not be blank");
        }

        // Convenção opcional: forçar prefixo ROLE_:
        // if (!trimmed.startsWith("ROLE_")) {
        //     trimmed = "ROLE_" + trimmed;
        // }

        return trimmed;
    }

    // equals/hashCode por name (identidade lógica do role).
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role role)) return false;
        return name.equals(role.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    // toString seguro e objetivo (não lista permissões para evitar logs extensos).
    @Override
    public String toString() {
        return "Role{" +
                "name='" + name + '\'' +
                ", permissionsCount=" + permissions.size() +
                '}';
    }
}

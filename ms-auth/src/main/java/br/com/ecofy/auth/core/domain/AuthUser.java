package br.com.ecofy.auth.core.domain;

import br.com.ecofy.auth.core.domain.enums.AuthUserStatus;
import br.com.ecofy.auth.core.domain.valueobject.AuthUserId;
import br.com.ecofy.auth.core.domain.valueobject.EmailAddress;
import br.com.ecofy.auth.core.domain.valueobject.PasswordHash;

import java.time.Instant;
import java.util.*;

// Raiz de agregado do usuário de autenticação, concentrando estado e regras de negócio de identidade, senha, status e permissões.
public class AuthUser {

    private final AuthUserId id;
    private final EmailAddress email;
    private PasswordHash passwordHash;
    private AuthUserStatus status;
    private boolean emailVerified;
    private String firstName;
    private String lastName;
    private String locale;
    private final Set<Role> roles;
    private final Set<Permission> directPermissions;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant lastLoginAt;
    private int failedLoginAttempts;

    // Reconstrói o agregado AuthUser a partir de dados persistidos, garantindo invariantes básicas do domínio.
    public AuthUser(AuthUserId id,
                    EmailAddress email,
                    PasswordHash passwordHash,
                    AuthUserStatus status,
                    boolean emailVerified,
                    String firstName,
                    String lastName,
                    String locale,
                    Set<Role> roles,
                    Set<Permission> directPermissions,
                    Instant createdAt,
                    Instant updatedAt,
                    Instant lastLoginAt,
                    int failedLoginAttempts) {

        this.id = Objects.requireNonNull(id, "id must not be null");
        this.email = Objects.requireNonNull(email, "email must not be null");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.emailVerified = emailVerified;
        this.firstName = firstName;
        this.lastName = lastName;
        this.locale = (locale != null && !locale.isBlank()) ? locale : "pt-BR";

        this.roles = roles != null ? new HashSet<>(roles) : new HashSet<>();
        this.directPermissions = directPermissions != null ? new HashSet<>(directPermissions) : new HashSet<>();

        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        this.lastLoginAt = lastLoginAt;
        this.failedLoginAttempts = Math.max(failedLoginAttempts, 0);
    }

    // Cria um novo usuário em estado pendente, com timestamps iniciais e roles informadas (ou vazias).
    public static AuthUser newPendingUser(EmailAddress email,
                                          PasswordHash passwordHash,
                                          String firstName,
                                          String lastName,
                                          String locale,
                                          Set<Role> roles) {
        Instant now = Instant.now();
        String effectiveLocale = (locale != null && !locale.isBlank()) ? locale : "pt-BR";

        return new AuthUser(
                AuthUserId.newId(),
                email,
                passwordHash,
                AuthUserStatus.PENDING_EMAIL_CONFIRMATION,
                false,
                firstName,
                lastName,
                effectiveLocale,
                roles != null ? roles : Set.of(),
                Set.of(),
                now,
                now,
                null,
                0
        );
    }

    // Retorna o identificador do usuário no domínio.
    public AuthUserId id() {
        return id;
    }

    // Retorna o e-mail do usuário (value object) para leitura/validação externa.
    public EmailAddress email() {
        return email;
    }

    // Retorna o hash atual da senha para validação (nunca a senha em texto puro).
    public PasswordHash passwordHash() {
        return passwordHash;
    }

    // Retorna o status atual do usuário (ativo, bloqueado, locked, etc.).
    public AuthUserStatus status() {
        return status;
    }

    // Indica se o e-mail do usuário já foi confirmado.
    public boolean isEmailVerified() {
        return emailVerified;
    }

    // Retorna o primeiro nome do usuário.
    public String firstName() {
        return firstName;
    }

    // Retorna o sobrenome do usuário.
    public String lastName() {
        return lastName;
    }

    // Retorna o locale efetivo do usuário (default pt-BR quando ausente).
    public String locale() {
        return locale;
    }

    // Retorna a data/hora de criação do usuário (imutável).
    public Instant createdAt() {
        return createdAt;
    }

    // Retorna a data/hora da última atualização do agregado.
    public Instant updatedAt() {
        return updatedAt;
    }

    // Retorna a data/hora do último login bem-sucedido (ou null se nunca logou).
    public Instant lastLoginAt() {
        return lastLoginAt;
    }

    // Retorna o contador atual de tentativas de login falhadas.
    public int failedLoginAttempts() {
        return failedLoginAttempts;
    }

    // Retorna as roles do usuário como visão somente-leitura.
    public Set<Role> roles() {
        return Collections.unmodifiableSet(roles);
    }

    // Retorna as permissões diretas do usuário (fora das roles) como visão somente-leitura.
    public Set<Permission> directPermissions() {
        return Collections.unmodifiableSet(directPermissions);
    }

    // Monta e retorna o nome completo a partir de firstName/lastName com normalização de espaços.
    public String fullName() {
        String first = Objects.toString(firstName, "").trim();
        String last = Objects.toString(lastName, "").trim();
        return (first + " " + last).trim();
    }

    // Confirma o e-mail, atualiza o status quando aplicável e rejeita confirmação para usuários bloqueados/deletados.
    public void confirmEmail() {
        if (status == AuthUserStatus.BLOCKED || status == AuthUserStatus.DELETED) {
            throw new IllegalStateException("User is not eligible to confirm email");
        }
        this.emailVerified = true;
        if (status == AuthUserStatus.PENDING_EMAIL_CONFIRMATION) {
            this.status = AuthUserStatus.ACTIVE;
        }
        touch();
    }

    // Troca a senha do usuário, zera tentativas falhas e atualiza o timestamp de modificação.
    public void changePassword(PasswordHash newPasswordHash) {
        this.passwordHash = Objects.requireNonNull(newPasswordHash, "newPasswordHash must not be null");
        this.failedLoginAttempts = 0;
        touch();
    }

    // Registra login bem-sucedido, limpando tentativas falhas e atualizando lastLoginAt.
    public void registerSuccessfulLogin() {
        this.failedLoginAttempts = 0;
        this.lastLoginAt = Instant.now();
        touch();
    }

    // Registra tentativa de login falha e bloqueia (LOCKED) ao atingir o limite configurado.
    public void registerFailedLogin(int maxAttemptsBeforeLock) {
        if (maxAttemptsBeforeLock <= 0) {
            throw new IllegalArgumentException("maxAttemptsBeforeLock must be greater than zero");
        }
        this.failedLoginAttempts++;
        if (failedLoginAttempts >= maxAttemptsBeforeLock) {
            this.status = AuthUserStatus.LOCKED;
        }
        touch();
    }

    // Verifica se o usuário possui a permissão requerida via roles ou permissões diretas.
    public boolean hasPermission(String permissionName) {
        Permission required = new Permission(permissionName, null, "*");
        return roles.stream().anyMatch(r -> r.implies(required))
                || directPermissions.stream().anyMatch(p -> p.implies(required));
    }

    // Adiciona uma role ao usuário e atualiza o timestamp de modificação.
    public void addRole(Role role) {
        this.roles.add(Objects.requireNonNull(role, "role must not be null"));
        touch();
    }

    // Adiciona uma permissão direta ao usuário e atualiza o timestamp de modificação.
    public void addDirectPermission(Permission permission) {
        this.directPermissions.add(Objects.requireNonNull(permission, "permission must not be null"));
        touch();
    }

    // Marca o usuário como BLOCKED para impedir operações sensíveis e acessos.
    public void block() {
        this.status = AuthUserStatus.BLOCKED;
        touch();
    }

    // Marca o usuário como DELETED (soft delete) para desativação lógica.
    public void delete() {
        this.status = AuthUserStatus.DELETED;
        touch();
    }

    // Atualiza o updatedAt para refletir mudanças de estado no agregado.
    private void touch() {
        this.updatedAt = Instant.now();
    }

}

package br.com.ecofy.ms_users.core.domain;

import br.com.ecofy.ms_users.core.domain.enums.UserStatus;
import br.com.ecofy.ms_users.core.domain.valueobject.EmailAddress;
import br.com.ecofy.ms_users.core.domain.valueobject.ExternalAuthId;
import br.com.ecofy.ms_users.core.domain.valueobject.PhoneNumber;
import br.com.ecofy.ms_users.core.domain.valueobject.UserId;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;

@Getter
@Builder(toBuilder = true)
public class EcoUserProfile {

    private final UserId id;
    private final ExternalAuthId externalAuthId;

    private final String fullName;
    private final EmailAddress email;
    private final PhoneNumber phone;

    private final UserStatus status;

    // Campos adicionados a partir do "modelo novo"
    private final boolean emailVerified;
    private final String locale;

    private final Instant createdAt;
    private final Instant updatedAt;

    /**
     * Factory para criação de perfil a partir do Auth (fonte de verdade).
     * - Gera um novo UserId
     * - Define timestamps (createdAt/updatedAt) como "now"
     * - Mantém campos opcionais (fullName pode ser null, etc.)
     */
    public static EcoUserProfile newFromAuth(
            ExternalAuthId externalAuthId,
            EmailAddress email,
            String fullName,
            UserStatus status,
            boolean emailVerified,
            String locale
    ) {
        Instant now = Instant.now();

        // externalAuthId e email normalmente são obrigatórios em sync de Auth,
        // mas aqui não forçamos exception para manter compatível com eventos incompletos.
        return EcoUserProfile.builder()
                .id(UserId.newId())
                .externalAuthId(externalAuthId)
                .email(email)
                .fullName(fullName)
                .status(status != null ? status : UserStatus.ACTIVE)
                .emailVerified(emailVerified)
                .locale(locale != null && !locale.isBlank() ? locale : "pt-BR")
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    /**
     * Atualiza o perfil existente com dados sincronizados do Auth.
     * Regra:
     * - Preenche somente quando vier valor (email/fullName/status/locale)
     * - emailVerified é "fonte de verdade" do Auth (aplica sempre)
     * - Mantém id/externalAuthId/createdAt/phone (phone não vem do Auth normalmente)
     * - Atualiza updatedAt para "now"
     */
    public EcoUserProfile withSyncedAuthData(
            EmailAddress email,
            String fullName,
            UserStatus status,
            boolean emailVerified,
            String locale
    ) {
        Instant now = Instant.now();

        return this.toBuilder()
                .email(email != null ? email : this.email)
                .fullName(fullName != null ? fullName : this.fullName)
                .status(status != null ? status : this.status)
                .emailVerified(emailVerified)
                .locale(locale != null && !locale.isBlank() ? locale : this.locale)
                .updatedAt(now)
                .build();
    }

    /**
     * Helper opcional (mas bem útil) para garantir defaults coerentes ao construir perfis
     * por caminhos que não passam por newFromAuth().
     *
     * Use isso se você quiser normalizar ao salvar, por exemplo, em um serviço de domínio.
     */
    public EcoUserProfile withDefaultsIfMissing() {
        return this.toBuilder()
                .status(this.status != null ? this.status : UserStatus.PENDING)
                .locale(this.locale != null && !this.locale.isBlank() ? this.locale : "pt-BR")
                .build();
    }

    /**
     * Validação mínima opcional (não joga exception automaticamente).
     * Se você usa invariantes fortes no domínio, pode trocar para exception de domínio.
     */
    public void validate() {
        Objects.requireNonNull(id, "id must not be null");
        // Se quiser exigir invariantes:
        // Objects.requireNonNull(externalAuthId, "externalAuthId must not be null");
        // Objects.requireNonNull(email, "email must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }
}

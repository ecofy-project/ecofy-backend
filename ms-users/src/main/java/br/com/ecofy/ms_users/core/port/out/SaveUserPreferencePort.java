package br.com.ecofy.ms_users.core.port.out;

import br.com.ecofy.ms_users.core.domain.UserPreference;
import br.com.ecofy.ms_users.core.domain.enums.PreferenceKey;
import br.com.ecofy.ms_users.core.domain.valueobject.UserId;

import java.util.Collection;
import java.util.List;

public interface SaveUserPreferencePort {
    List<UserPreference> upsertAll(List<UserPreference> prefs);

    /**
     * Remove as preferências das chaves informadas para o usuário.
     * Usado para "limpar" uma preferência (política: valor vazio => remoção).
     *
     * @return número de preferências efetivamente removidas.
     */
    int deleteByUserIdAndKeys(UserId userId, Collection<PreferenceKey> keys);
}

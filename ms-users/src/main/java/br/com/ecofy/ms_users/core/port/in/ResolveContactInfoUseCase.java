package br.com.ecofy.ms_users.core.port.in;

import br.com.ecofy.ms_users.core.application.result.ContactInfoResult;

import java.util.UUID;

public interface ResolveContactInfoUseCase {
    ContactInfoResult resolve(UUID userId);
}

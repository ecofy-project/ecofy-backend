package br.com.ecofy.ms_users.core.application.command;

import br.com.ecofy.ms_users.core.domain.enums.PreferenceKey;

import java.util.Map;
import java.util.UUID;

public record UpdatePreferencesCommand(

        UUID userId,

        Map<PreferenceKey, String> preferences,

        String idempotencyKey

) { }

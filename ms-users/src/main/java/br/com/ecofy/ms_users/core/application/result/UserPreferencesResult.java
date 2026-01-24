package br.com.ecofy.ms_users.core.application.result;

import java.util.Map;
import java.util.UUID;

public record UserPreferencesResult(

        UUID userId,

        Map<String, String> preferences

) { }
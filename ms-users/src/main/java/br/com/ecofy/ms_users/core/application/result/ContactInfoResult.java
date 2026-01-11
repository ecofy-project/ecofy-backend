package br.com.ecofy.ms_users.core.application.result;

import java.util.UUID;

public record ContactInfoResult(
        UUID userId,
        String email,
        String phone,
        String notifyChannels
) {}

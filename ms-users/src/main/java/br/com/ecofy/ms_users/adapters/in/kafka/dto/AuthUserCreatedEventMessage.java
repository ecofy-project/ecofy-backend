package br.com.ecofy.ms_users.adapters.in.kafka.dto;

import java.util.UUID;

public record AuthUserCreatedEventMessage(

        UUID userId,

        String externalAuthId,

        String fullName,

        String email,

        String phone,

        MessageMetadata metadata

) { }
package br.com.ecofy.auth.adapters.in.web.dto.response;

import java.util.Map;

public record ValidateTokenResponse(

        boolean valid,
        Map<String, Object> claims

) { }

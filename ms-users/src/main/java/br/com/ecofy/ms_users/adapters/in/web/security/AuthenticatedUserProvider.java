package br.com.ecofy.ms_users.adapters.in.web.security;

import java.util.Optional;

public interface AuthenticatedUserProvider {

    Optional<String> currentAuthUserId();

    String requireAuthUserId();

    boolean isServiceToken();

    boolean hasRole(String role);
}

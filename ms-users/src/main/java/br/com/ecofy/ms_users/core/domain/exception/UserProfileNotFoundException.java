package br.com.ecofy.ms_users.core.domain.exception;

import java.util.UUID;

public class UserProfileNotFoundException extends RuntimeException {

    public UserProfileNotFoundException(UUID userId) {
        super("User profile not found for authUserId: " + userId);
    }

    private UserProfileNotFoundException(String message) {
        super(message);
    }

    public static UserProfileNotFoundException forAuthenticatedUser() {
        return new UserProfileNotFoundException("User profile not found for the authenticated user");
    }
}

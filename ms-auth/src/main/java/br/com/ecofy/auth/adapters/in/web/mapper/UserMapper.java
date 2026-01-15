package br.com.ecofy.auth.adapters.in.web.mapper;

import br.com.ecofy.auth.adapters.in.web.dto.response.UserResponse;
import br.com.ecofy.auth.core.domain.AuthUser;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class UserMapper {

    public static UserResponse toResponse(AuthUser user) {
        Objects.requireNonNull(user, "user must not be null");

        Set<String> roles = user.roles().stream()
                .map(br.com.ecofy.auth.core.domain.Role::name)
                .collect(Collectors.toUnmodifiableSet());

        Set<String> permissions = user.directPermissions().stream()
                .map(br.com.ecofy.auth.core.domain.Permission::name)
                .collect(Collectors.toUnmodifiableSet());

        return new UserResponse(
                user.id().value().toString(),
                user.email().value(),
                user.fullName(),
                user.status().name(),
                user.isEmailVerified(),
                roles,
                permissions,
                user.createdAt(),
                user.updatedAt(),
                user.lastLoginAt()
        );
    }

    static List<UserResponse> toResponseList(List<AuthUser> users) {
        if (users == null || users.isEmpty()) {
            return List.of();
        }
        return Collections.unmodifiableList(
                users.stream()
                        .filter(Objects::nonNull)
                        .map(UserMapper::toResponse)
                        .toList()
        );
    }
}

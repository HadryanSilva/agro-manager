package br.com.hadryan.agro.manager.user.dto;

import br.com.hadryan.agro.manager.user.domain.User;
import br.com.hadryan.agro.manager.user.domain.UserRole;
import br.com.hadryan.agro.manager.user.domain.UserStatus;
import java.time.Instant;

/**
 * Wire-safe view of {@link User}. Excludes {@code passwordHash} and
 * {@code googleSubject} — those are auth internals and must never leave
 * the server. {@code hasPassword} / {@code hasGoogle} flags tell the
 * client what credentials exist without exposing the values.
 */
public record UserResponse(
        Long id,
        Long accountId,
        String email,
        String name,
        UserRole role,
        UserStatus status,
        boolean hasPassword,
        boolean hasGoogle,
        Instant createdAt,
        Instant updatedAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getAccount().getId(),
                user.getEmail(),
                user.getName(),
                user.getRole(),
                user.getStatus(),
                user.getPasswordHash() != null,
                user.getGoogleSubject() != null,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
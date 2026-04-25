package br.com.hadryan.agro.manager.domain.user;

import br.com.hadryan.agro.manager.domain.user.AuthProvider;
import br.com.hadryan.agro.manager.domain.user.User;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Dados do perfil do usuário retornados ao cliente.
 * authProvider indica se a conta é LOCAL ou GOOGLE — usado no frontend
 * para exibir ou ocultar a seção de alteração de senha.
 */
public record UserProfileResponse(
        UUID id,
        String name,
        String email,
        String avatarUrl,
        AuthProvider authProvider,
        boolean emailVerified,
        LocalDateTime createdAt
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getAvatarUrl(),
                user.getAuthProvider(),
                user.isEmailVerified(),
                user.getCreatedAt()
        );
    }
}
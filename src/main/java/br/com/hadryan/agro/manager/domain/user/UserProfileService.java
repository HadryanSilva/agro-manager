package br.com.hadryan.agro.manager.domain.user;

import br.com.hadryan.agro.manager.shared.exception.BusinessException;
import br.com.hadryan.agro.manager.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Serviço responsável pela visualização e atualização do perfil do usuário autenticado.
 * Alteração de senha é permitida apenas para contas com authProvider LOCAL.
 */
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        return UserProfileResponse.from(findUser(userId));
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = findUser(userId);
        user.setName(request.name());
        return UserProfileResponse.from(userRepository.save(user));
    }

    /**
     * Altera a senha do usuário.
     * Bloqueado para contas OAuth2 — essas contas não possuem senha local.
     */
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = findUser(userId);

        // Usuários OAuth2 não possuem senha gerenciada pela aplicação
        if (user.getAuthProvider() != AuthProvider.LOCAL) {
            throw new BusinessException(
                    "Alteração de senha não disponível para contas vinculadas ao Google",
                    HttpStatus.BAD_REQUEST
            );
        }

        // Valida a senha atual antes de permitir a troca
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException("Senha atual incorreta", HttpStatus.BAD_REQUEST);
        }

        // Impede reutilização da mesma senha
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BusinessException("A nova senha não pode ser igual à senha atual", HttpStatus.BAD_REQUEST);
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", "id", userId));
    }
}
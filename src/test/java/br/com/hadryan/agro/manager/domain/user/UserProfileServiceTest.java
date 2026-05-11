package br.com.hadryan.agro.manager.domain.user;

import br.com.hadryan.agro.manager.shared.exception.BusinessException;
import br.com.hadryan.agro.manager.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários de UserProfileServiceImpl.
 * Cobre: leitura de perfil, atualização e alteração de senha com casos de erro.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserProfileService — Perfil do usuário")
class UserProfileServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private UserProfileServiceImpl userProfileService;

    private User buildLocalUser(UUID id) {
        return User.builder()
                .id(id)
                .name("Maria Silva")
                .email("maria@test.com")
                .passwordHash("hashed-password")
                .authProvider(AuthProvider.LOCAL)
                .emailVerified(true)
                .build();
    }

    // ── getProfile ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Deve retornar perfil do usuário existente")
    void getProfile_success() {
        UUID userId = UUID.randomUUID();
        User user = buildLocalUser(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserProfileResponse response = userProfileService.getProfile(userId);

        assertThat(response.name()).isEqualTo("Maria Silva");
        assertThat(response.email()).isEqualTo("maria@test.com");
        assertThat(response.authProvider()).isEqualTo(AuthProvider.LOCAL);
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException para usuário inexistente")
    void getProfile_notFound() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.getProfile(userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── changePassword ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Deve alterar senha com sucesso para conta LOCAL")
    void changePassword_success() {
        UUID userId = UUID.randomUUID();
        User user = buildLocalUser(userId);
        ChangePasswordRequest request = new ChangePasswordRequest("senha-atual", "nova-senha-123");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("senha-atual", "hashed-password")).thenReturn(true);
        when(passwordEncoder.matches("nova-senha-123", "hashed-password")).thenReturn(false);
        when(passwordEncoder.encode("nova-senha-123")).thenReturn("novo-hash");
        when(userRepository.save(any())).thenReturn(user);

        userProfileService.changePassword(userId, request);

        verify(userRepository).save(user);
        assertThat(user.getPasswordHash()).isEqualTo("novo-hash");
    }

    @Test
    @DisplayName("Deve lançar BusinessException para conta OAuth2 (Google)")
    void changePassword_oauthAccount_rejected() {
        UUID userId = UUID.randomUUID();
        User googleUser = User.builder()
                .id(userId).name("Ana").email("ana@gmail.com")
                .authProvider(AuthProvider.GOOGLE).build();
        ChangePasswordRequest request = new ChangePasswordRequest("qualquer", "qualquer");

        when(userRepository.findById(userId)).thenReturn(Optional.of(googleUser));

        assertThatThrownBy(() -> userProfileService.changePassword(userId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Google");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar BusinessException quando senha atual está incorreta")
    void changePassword_wrongCurrentPassword() {
        UUID userId = UUID.randomUUID();
        User user = buildLocalUser(userId);
        ChangePasswordRequest request = new ChangePasswordRequest("errada", "nova-senha");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("errada", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> userProfileService.changePassword(userId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Senha atual incorreta");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar BusinessException quando nova senha é igual à atual")
    void changePassword_samePassword_rejected() {
        UUID userId = UUID.randomUUID();
        User user = buildLocalUser(userId);
        ChangePasswordRequest request = new ChangePasswordRequest("senha-atual", "senha-atual");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("senha-atual", "hashed-password")).thenReturn(true);

        assertThatThrownBy(() -> userProfileService.changePassword(userId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não pode ser igual");

        verify(userRepository, never()).save(any());
    }
}
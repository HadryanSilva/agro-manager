package br.com.hadryan.agro.manager.domain.auth;

import br.com.hadryan.agro.manager.config.JwtProperties;
import br.com.hadryan.agro.manager.domain.user.AuthProvider;
import br.com.hadryan.agro.manager.domain.user.User;
import br.com.hadryan.agro.manager.domain.user.UserRepository;
import br.com.hadryan.agro.manager.infra.security.JwtService;
import br.com.hadryan.agro.manager.shared.exception.BusinessException;
import br.com.hadryan.agro.manager.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários de AuthServiceImpl.
 * Cobre: registro, login, refresh token e casos de erro.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — Fluxos de autenticação")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtProperties jwtProperties;

    @InjectMocks private AuthServiceImpl authService;

    @BeforeEach
    void setup() {
        // lenient: stubs only reached on success paths — error-path tests don't call buildAuthResponse
        lenient().when(jwtProperties.expiration()).thenReturn(900_000L);
        lenient().when(jwtService.generateAccessToken(any(User.class))).thenReturn("access-token");
        lenient().when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh-token");
    }

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Deve registrar novo usuário e retornar tokens JWT")
    void register_success() {
        RegisterRequest request = new RegisterRequest("João", "joao@test.com", "senha123");

        when(userRepository.existsByEmailIgnoreCase("joao@test.com")).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.register(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Deve normalizar e-mail antes de verificar duplicidade e salvar usuário")
    void register_normalizesEmail() {
        RegisterRequest request = new RegisterRequest("João", "  Joao@Test.COM  ", "senha123");

        when(userRepository.existsByEmailIgnoreCase("joao@test.com")).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.register(request);

        verify(userRepository).existsByEmailIgnoreCase("joao@test.com");
        verify(userRepository).save(argThat(user -> "joao@test.com".equals(user.getEmail())));
    }

    @Test
    @DisplayName("Deve lançar BusinessException quando e-mail já está cadastrado")
    void register_duplicateEmail() {
        RegisterRequest request = new RegisterRequest("João", "joao@test.com", "senha123");
        when(userRepository.existsByEmailIgnoreCase("joao@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("E-mail já cadastrado");

        verify(userRepository, never()).save(any());
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Deve autenticar e retornar tokens com credenciais válidas")
    void login_success() {
        LoginRequest request = new LoginRequest("joao@test.com", "senha123");
        User user = User.builder()
                .id(UUID.randomUUID()).email("joao@test.com")
                .authProvider(AuthProvider.LOCAL).build();

        when(userRepository.findByEmailIgnoreCase("joao@test.com")).thenReturn(Optional.of(user));

        AuthResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Deve normalizar e-mail antes de autenticar e buscar usuário")
    void login_normalizesEmail() {
        LoginRequest request = new LoginRequest("  Joao@Test.COM  ", "senha123");
        User user = User.builder()
                .id(UUID.randomUUID()).email("joao@test.com")
                .authProvider(AuthProvider.LOCAL).build();

        when(userRepository.findByEmailIgnoreCase("joao@test.com")).thenReturn(Optional.of(user));

        authService.login(request);

        verify(authenticationManager).authenticate(argThat(authentication ->
                authentication instanceof UsernamePasswordAuthenticationToken
                        && "joao@test.com".equals(authentication.getPrincipal())
                        && "senha123".equals(authentication.getCredentials())));
        verify(userRepository).findByEmailIgnoreCase("joao@test.com");
    }

    @Test
    @DisplayName("Deve propagar BadCredentialsException do AuthenticationManager")
    void login_badCredentials() {
        LoginRequest request = new LoginRequest("joao@test.com", "errada");

        doThrow(new BadCredentialsException("Credenciais inválidas"))
                .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        verify(userRepository, never()).findByEmailIgnoreCase(any());
    }

    // ── refresh ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Deve renovar tokens com refresh token válido")
    void refresh_success() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).email("joao@test.com")
                .authProvider(AuthProvider.LOCAL).build();
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");

        when(jwtService.extractUserIdIfValid("valid-refresh-token"))
                .thenReturn(Optional.of(userId.toString()));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        AuthResponse response = authService.refresh(request);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    @DisplayName("Deve lançar BusinessException quando refresh token é inválido")
    void refresh_invalidToken() {
        RefreshTokenRequest request = new RefreshTokenRequest("invalid-token");
        when(jwtService.extractUserIdIfValid("invalid-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("inválido ou expirado");

        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando usuário do token não existe mais")
    void refresh_userNotFound() {
        UUID userId = UUID.randomUUID();
        RefreshTokenRequest request = new RefreshTokenRequest("valid-token");

        when(jwtService.extractUserIdIfValid("valid-token"))
                .thenReturn(Optional.of(userId.toString()));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

package br.com.hadryan.agro.manager.domain.auth;

import br.com.hadryan.agro.manager.config.JwtProperties;
import br.com.hadryan.agro.manager.shared.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints públicos de autenticação.
 * Não exigem token JWT — liberados no SecurityConfig via /auth/**.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    public static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    private final AuthServiceImpl authService;
    private final JwtProperties jwtProperties;

    /**
     * Cria uma nova conta com e-mail e senha.
     * Retorna os tokens JWT para que o frontend já autentique o usuário.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest servletRequest) {

        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, refreshCookie(response.refreshToken(), servletRequest).toString())
                .body(ApiResponse.success("Conta criada com sucesso", response));
    }

    /**
     * Autentica o usuário com e-mail e senha.
     * Retorna access token (15 min) e refresh token (7 dias).
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest) {

        AuthResponse response = authService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(response.refreshToken(), servletRequest).toString())
                .body(ApiResponse.success(response));
    }

    /**
     * Renova o access token usando um refresh token válido.
     * Também retorna um novo refresh token (rotação de tokens).
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken,
            HttpServletRequest servletRequest) {

        AuthResponse response = authService.refresh(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(response.refreshToken(), servletRequest).toString())
                .body(ApiResponse.success(response));
    }

    private ResponseCookie refreshCookie(String refreshToken, HttpServletRequest request) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken)
                .httpOnly(true)
                .secure(request.isSecure())
                .sameSite("Lax")
                .path("/auth/refresh")
                .maxAge(jwtProperties.refreshExpiration() / 1000)
                .build();
    }
}

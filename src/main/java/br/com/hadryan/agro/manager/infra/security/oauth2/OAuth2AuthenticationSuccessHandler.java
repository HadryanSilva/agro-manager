package br.com.hadryan.agro.manager.infra.security.oauth2;

import br.com.hadryan.agro.manager.infra.security.JwtService;
import br.com.hadryan.agro.manager.infra.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Handler executado após autenticação OAuth2 bem-sucedida.
 * Gera os tokens JWT e redireciona o usuário para o frontend
 * com o access token como parâmetro de URL.
 *
 * O refresh token é enviado em cookie HttpOnly e usado somente pelo endpoint
 * /auth/refresh. O frontend extrai apenas o access token da URL e o mantém
 * em memória.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    private final JwtService jwtService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.jwt.refresh-expiration}")
    private long refreshExpiration;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        // Usa os dados já disponíveis no principal — elimina query ao banco desnecessária
        String accessToken  = jwtService.generateAccessToken(principal.getId(), principal.getEmail());
        String refreshToken = jwtService.generateRefreshToken(principal.getId(), principal.getEmail());

        ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken)
                .httpOnly(true)
                .secure(request.isSecure())
                .sameSite("Lax")
                .path("/auth/refresh")
                .maxAge(refreshExpiration / 1000)
                .build();

        String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth2/callback")
                .queryParam("accessToken", accessToken)
                .build()
                .toUriString();

        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}

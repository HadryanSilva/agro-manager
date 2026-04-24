package br.com.hadryan.agro.manager.infra.security.oauth2;

import br.com.hadryan.agro.manager.domain.user.UserRepository;
import br.com.hadryan.agro.manager.infra.security.JwtService;
import br.com.hadryan.agro.manager.infra.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Handler executado após autenticação OAuth2 bem-sucedida.
 * Gera os tokens JWT e redireciona o usuário para o frontend
 * com o accessToken como parâmetro de URL.
 *
 * O frontend extrai o token da URL, armazena em memória e descarta da barra de endereços.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        // Carrega a entidade User completa para geração do token
        userRepository.findById(principal.getId()).ifPresentOrElse(user -> {
            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);

            // Redireciona para o frontend com os tokens como parâmetros de URL
            String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth2/callback")
                    .queryParam("accessToken", accessToken)
                    .queryParam("refreshToken", refreshToken)
                    .build().toUriString();

            try {
                getRedirectStrategy().sendRedirect(request, response, redirectUrl);
            } catch (IOException e) {
                log.error("Erro ao redirecionar após OAuth2: {}", e.getMessage());
            }
        }, () -> log.error("Usuário OAuth2 não encontrado no banco após autenticação: {}", principal.getId()));
    }
}
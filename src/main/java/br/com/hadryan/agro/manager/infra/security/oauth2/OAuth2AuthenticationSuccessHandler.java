package br.com.hadryan.agro.manager.infra.security.oauth2;

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
 * com os tokens como parâmetros de URL.
 *
 * O frontend extrai os tokens da URL, armazena em memória e limpa a barra de endereços.
 *
 * NOTA DE SEGURANÇA: tokens em query params ficam expostos em:
 *   - histórico do browser
 *   - logs de servidor/proxy (access logs)
 *   - header Referer de navegações subsequentes
 *
 * A solução recomendada a longo prazo é substituir os query params por cookies
 * HttpOnly + Secure + SameSite=Strict, o que requer coordenação com o frontend.
 * Enquanto isso, o refreshToken tem vida de 7 dias e o accessToken de 15 minutos,
 * minimizando a janela de exposição.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        // Usa os dados já disponíveis no principal — elimina query ao banco desnecessária
        String accessToken  = jwtService.generateAccessToken(principal.getId(), principal.getEmail());
        String refreshToken = jwtService.generateRefreshToken(principal.getId(), principal.getEmail());

        String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth2/callback")
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .build()
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
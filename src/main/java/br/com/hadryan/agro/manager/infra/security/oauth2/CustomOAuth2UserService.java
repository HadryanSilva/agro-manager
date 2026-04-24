package br.com.hadryan.agro.manager.infra.security.oauth2;

import br.com.hadryan.agro.manager.domain.user.AuthProvider;
import br.com.hadryan.agro.manager.domain.user.User;
import br.com.hadryan.agro.manager.domain.user.UserRepository;
import br.com.hadryan.agro.manager.infra.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Serviço que processa o retorno do Google após autenticação OAuth2.
 * Cria o usuário caso seja o primeiro acesso, ou atualiza os dados se já existir.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String providerId = (String) attributes.get("sub");
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String avatarUrl = (String) attributes.get("picture");
        Boolean emailVerified = (Boolean) attributes.getOrDefault("email_verified", false);

        if (email == null) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_not_found"),
                    "E-mail não fornecido pelo Google"
            );
        }

        User user = userRepository.findByEmail(email)
                .map(existing -> updateExistingUser(existing, name, avatarUrl, providerId, emailVerified))
                .orElseGet(() -> createNewUser(email, name, avatarUrl, providerId, emailVerified));

        return UserPrincipal.fromOAuth2(user, attributes);
    }

    // Atualiza dados do usuário existente que pode ter se cadastrado via e-mail
    private User updateExistingUser(User user, String name, String avatarUrl,
                                    String providerId, boolean emailVerified) {
        user.setName(name);
        user.setAvatarUrl(avatarUrl);
        user.setProviderId(providerId);
        user.setEmailVerified(emailVerified);
        return userRepository.save(user);
    }

    // Cria um novo usuário a partir dos dados do Google
    private User createNewUser(String email, String name, String avatarUrl,
                               String providerId, boolean emailVerified) {
        User newUser = User.builder()
                .email(email)
                .name(name)
                .avatarUrl(avatarUrl)
                .providerId(providerId)
                .authProvider(AuthProvider.GOOGLE)
                .emailVerified(emailVerified)
                .build();
        return userRepository.save(newUser);
    }
}
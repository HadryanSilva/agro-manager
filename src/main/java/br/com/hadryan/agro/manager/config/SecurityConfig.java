package br.com.hadryan.agro.manager.config;

import br.com.hadryan.agro.manager.infra.security.JwtAuthenticationFilter;
import br.com.hadryan.agro.manager.infra.security.oauth2.CustomOAuth2UserService;
import br.com.hadryan.agro.manager.infra.security.oauth2.OAuth2AuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuração central de segurança da aplicação.
 * Define: rotas públicas, JWT stateless, OAuth2 com Google e CORS.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final UserDetailsService userDetailsService;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Desabilita CSRF pois a autenticação é stateless via JWT
                .csrf(AbstractHttpConfigurer::disable)

                // CORS configurado via bean abaixo
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Sessão stateless — o estado é mantido apenas no token JWT
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Registra o DaoAuthenticationProvider localmente no HttpSecurity,
                // sem expô-lo como @Bean — elimina o WARN do Spring Security sobre
                // ambiguidade entre AuthenticationProvider e UserDetailsService beans
                .authenticationProvider(daoAuthenticationProvider())

                // Definição de rotas públicas e protegidas
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/auth/**",
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/actuator/health",
                                "/actuator/prometheus",
                                "/actuator/info"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/invites/**").permitAll()
                        .anyRequest().authenticated()
                )

                // Configuração do fluxo OAuth2 com Google
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo ->
                                userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                )

                // Sobrescreve o comportamento padrão do OAuth2 apenas para requisições de API.
                // Distingue pelo header Authorization (Bearer token) ou Accept: application/json.
                // Requisições do browser (callback OAuth2 do Google) não têm esses headers
                // e continuam com o comportamento padrão — redireciona para login.
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            String authHeader   = request.getHeader("Authorization");
                            String acceptHeader = request.getHeader("Accept");

                            boolean isApiRequest = (authHeader != null && authHeader.startsWith("Bearer "))
                                    || (acceptHeader != null && acceptHeader.contains("application/json"));

                            if (isApiRequest) {
                                // Requisição de API com token expirado — retorna 401 para o Axios fazer refresh
                                response.setStatus(401);
                                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                                response.setCharacterEncoding("UTF-8");
                                response.getWriter().write(
                                        "{\"status\":401,\"message\":\"Token expirado ou inválido\"}"
                                );
                            } else {
                                // Fluxo de browser (callback OAuth2) — redireciona para o login
                                response.sendRedirect("/oauth2/authorization/google");
                            }
                        })
                )

                // Filtro JWT executado antes do filtro de autenticação padrão
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // Método privado — não é um @Bean, apenas configura o provider localmente
    // O AuthService recebe o AuthenticationManager via AuthenticationConfiguration,
    // que enxerga o provider registrado no HttpSecurity acima
    private DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
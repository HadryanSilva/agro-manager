package br.com.hadryan.agro.manager.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.TimeValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

/**
 * Substitui o RestTemplate padrão do DefaultOAuth2UserService por uma
 * implementação com Apache HttpClient e pool de conexões.
 *
 * Motivo: o SimpleClientHttpRequestFactory padrão cria um novo socket SSL
 * por requisição e tenta criar uma thread de notificação por handshake
 * (HandshakeCompletedNotify-Thread). Em instâncias com limite de threads
 * reduzido (ulimits), isso causa OutOfMemoryError após múltiplos logins.
 *
 * O Apache HttpClient reutiliza conexões do pool, eliminando a criação
 * de novas threads SSL e resolvendo definitivamente o problema.
 */
@Configuration
public class OAuth2HttpClientConfig {

    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService() {
        DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

        // Pool de conexões — reutiliza sockets SSL em vez de criar novos por requisição
        PoolingHttpClientConnectionManager connectionManager =
                new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(20);
        connectionManager.setDefaultMaxPerRoute(10);

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(10, TimeUnit.SECONDS)
                .setResponseTimeout(15, TimeUnit.SECONDS)
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                // Remove conexões obsoletas automaticamente
                .evictExpiredConnections()
                .evictIdleConnections(TimeValue.of(30, TimeUnit.SECONDS))
                .build();

        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        RestTemplate restTemplate = new RestTemplate(factory);
        delegate.setRestOperations(restTemplate);

        return delegate;
    }
}
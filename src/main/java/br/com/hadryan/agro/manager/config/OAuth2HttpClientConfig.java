package br.com.hadryan.agro.manager.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.TimeValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configura o RestTemplate do CustomOAuth2UserService com Apache HttpClient e pool de conexões.
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

    @Bean(destroyMethod = "shutdown")
    public PoolingHttpClientConnectionManager oauth2ConnectionManager() {
        PoolingHttpClientConnectionManager connectionManager =
                new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(20);
        connectionManager.setDefaultMaxPerRoute(10);
        return connectionManager;
    }

    @Bean(destroyMethod = "close")
    public CloseableHttpClient oauth2HttpClient(PoolingHttpClientConnectionManager oauth2ConnectionManager) {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(10, TimeUnit.SECONDS)
                .setResponseTimeout(15, TimeUnit.SECONDS)
                .build();

        return HttpClients.custom()
                .setConnectionManager(oauth2ConnectionManager)
                .setDefaultRequestConfig(requestConfig)
                // Remove conexões obsoletas automaticamente
                .evictExpiredConnections()
                .evictIdleConnections(TimeValue.of(30, TimeUnit.SECONDS))
                .build();
    }
}
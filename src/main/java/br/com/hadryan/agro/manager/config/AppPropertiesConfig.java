package br.com.hadryan.agro.manager.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registra as classes @ConfigurationProperties da aplicação.
 * O uso de @EnableConfigurationProperties (em vez de @Component nas records)
 * é o padrão recomendado pelo Spring Boot para configurações tipadas.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class AppPropertiesConfig {}
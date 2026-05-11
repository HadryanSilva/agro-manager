package br.com.hadryan.agro.manager.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Propriedades de configuração JWT agrupadas e validadas no startup.
 *
 * Substitui injeção via @Value espalhada em JwtService e AuthService,
 * centralizando o contrato de configuração em um único lugar.
 *
 * Se JWT_SECRET não estiver configurado ou for menor que 32 caracteres,
 * a aplicação falha no startup com mensagem clara — antes de aceitar requisições.
 */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(

        // Segredo mínimo de 32 caracteres para garantir HS256 adequado
        @NotBlank(message = "app.jwt.secret é obrigatório")
        @Size(min = 32, message = "app.jwt.secret deve ter no mínimo 32 caracteres")
        String secret,

        // Validade do access token em milissegundos (padrão: 15 min = 900000 ms)
        @Min(value = 60_000, message = "app.jwt.expiration deve ser no mínimo 60000 ms (1 minuto)")
        long expiration,

        // Validade do refresh token em milissegundos (padrão: 7 dias = 604800000 ms)
        @Min(value = 60_000, message = "app.jwt.refresh-expiration deve ser no mínimo 60000 ms")
        long refreshExpiration
) {}
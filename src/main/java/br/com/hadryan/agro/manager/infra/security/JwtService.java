package br.com.hadryan.agro.manager.infra.security;

import br.com.hadryan.agro.manager.config.JwtProperties;
import br.com.hadryan.agro.manager.domain.user.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Serviço responsável por gerar e validar tokens JWT.
 * Utiliza HMAC-SHA256 como algoritmo de assinatura.
 *
 * A chave de assinatura é computada uma única vez no startup (@PostConstruct)
 * e reutilizada em todas as operações subsequentes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    // Chave computada uma única vez no startup — evita recriação a cada operação JWT
    private SecretKey signingKey;

    @PostConstruct
    private void initSigningKey() {
        this.signingKey = Keys.hmacShaKeyFor(
                jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    // ── Geração de tokens ─────────────────────────────────────────────────────

    public String generateAccessToken(User user) {
        return buildToken(user.getId(), user.getEmail(), jwtProperties.expiration());
    }

    public String generateRefreshToken(User user) {
        return buildToken(user.getId(), user.getEmail(), jwtProperties.refreshExpiration());
    }

    /**
     * Overload que aceita id e email diretamente — evita carregamento desnecessário
     * da entidade User quando os dados já estão disponíveis (ex: fluxo OAuth2).
     */
    public String generateAccessToken(UUID userId, String email) {
        return buildToken(userId, email, jwtProperties.expiration());
    }

    public String generateRefreshToken(UUID userId, String email) {
        return buildToken(userId, email, jwtProperties.refreshExpiration());
    }

    private String buildToken(UUID userId, String email, long expirationMs) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(signingKey)
                .compact();
    }

    // ── Validação e extração ──────────────────────────────────────────────────

    /**
     * Valida o token e extrai o userId em uma única operação de parse.
     * Elimina a necessidade de chamar isTokenValid() + extractUserId() separadamente.
     *
     * @return Optional com o userId se o token for válido; Optional.empty() caso contrário
     */
    public Optional<String> extractUserIdIfValid(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims.getSubject());
        } catch (ExpiredJwtException e) {
            log.debug("Token JWT expirado");
        } catch (UnsupportedJwtException e) {
            log.debug("Token JWT não suportado");
        } catch (MalformedJwtException e) {
            log.debug("Token JWT malformado");
        } catch (SecurityException e) {
            log.debug("Assinatura JWT inválida");
        } catch (IllegalArgumentException e) {
            log.debug("Token JWT vazio ou nulo");
        }
        return Optional.empty();
    }

    /** Mantido para compatibilidade — prefira extractUserIdIfValid() quando possível. */
    public boolean isTokenValid(String token) {
        return extractUserIdIfValid(token).isPresent();
    }

    public String extractUserId(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claimsResolver.apply(claims);
    }
}
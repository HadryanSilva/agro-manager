package br.com.hadryan.agro.manager.infra.security;

import br.com.hadryan.agro.manager.domain.user.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

/**
 * Serviço responsável por gerar e validar tokens JWT.
 * Utiliza HMAC-SHA256 como algoritmo de assinatura.
 */
@Slf4j
@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration}")
    private long expiration;

    @Value("${app.jwt.refresh-expiration}")
    private long refreshExpiration;

    // Deriva a chave de assinatura a partir do segredo configurado
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        return buildToken(user.getId(), user.getEmail(), expiration);
    }

    public String generateRefreshToken(User user) {
        return buildToken(user.getId(), user.getEmail(), refreshExpiration);
    }

    private String buildToken(UUID userId, String email, long expirationMs) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUserId(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
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
        return false;
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claimsResolver.apply(claims);
    }
}
package br.com.hadryan.agro.manager.infra.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-IP sliding-window rate limiter for GET /invites/code/{code}.
 * Prevents brute-force enumeration of active invite codes.
 * Runs before the security filter chain via @Order(Ordered.HIGHEST_PRECEDENCE + 1).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class InviteCodeRateLimitFilter extends OncePerRequestFilter {

    private static final int  MAX_REQUESTS = 10;
    private static final long WINDOW_MS    = 60_000L;

    // value: long[]{requestCount, windowStartMs}
    private final ConcurrentHashMap<String, long[]> windows = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/invites/code/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws IOException, ServletException {

        String ip  = resolveClientIp(request);
        long   now = System.currentTimeMillis();

        if (!isAllowed(ip, now)) {
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(
                    "{\"status\":429,\"message\":\"Muitas tentativas. Aguarde antes de tentar novamente.\"}"
            );
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isAllowed(String ip, long now) {
        long[] slot = {0};
        windows.compute(ip, (k, v) -> {
            if (v == null || now - v[1] >= WINDOW_MS) {
                slot[0] = 1;
                return new long[]{1, now};
            }
            slot[0] = ++v[0];
            return v;
        });
        return slot[0] <= MAX_REQUESTS;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

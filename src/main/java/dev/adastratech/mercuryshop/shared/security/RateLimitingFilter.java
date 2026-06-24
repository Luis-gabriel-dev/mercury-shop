package dev.adastratech.mercuryshop.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.adastratech.mercuryshop.shared.exception.ApiError;
import dev.adastratech.mercuryshop.shared.web.RequestIdFilter;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Rate limiting nos endpoints sensíveis de autenticação (briefing seção 7.6).
 * Limita por IP+rota; ao estourar, responde 429 com Retry-After no formato de erro padrão.
 */
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Set<String> LIMITED_PATHS = Set.of(
            "/v1/auth/login", "/v1/auth/register", "/v1/auth/forgot-password");

    private final LettuceBasedProxyManager<String> proxyManager;
    private final long capacity;
    private final Duration refillPeriod;
    private final ObjectMapper objectMapper;

    public RateLimitingFilter(LettuceBasedProxyManager<String> proxyManager,
                              SecurityProperties properties, ObjectMapper objectMapper) {
        this.proxyManager = proxyManager;
        this.capacity = properties.rateLimit().capacity();
        this.refillPeriod = properties.rateLimit().refillPeriod();
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("POST".equals(request.getMethod()) && LIMITED_PATHS.contains(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String key = "rl:" + request.getRequestURI() + ":" + clientIp(request);
        Bucket bucket = proxyManager.builder().build(key, this::bucketConfiguration);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
            return;
        }
        long retryAfter = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(retryAfter));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ApiError body = ApiError.of("RATE_LIMITED",
                "Muitas requisições; tente novamente em instantes.", MDC.get(RequestIdFilter.MDC_KEY));
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private BucketConfiguration bucketConfiguration() {
        return BucketConfiguration.builder()
                .addLimit(limit -> limit.capacity(capacity).refillGreedy(capacity, refillPeriod))
                .build();
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

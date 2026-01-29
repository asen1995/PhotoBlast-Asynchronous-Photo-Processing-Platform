package com.photoblast.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.time.Duration;
import java.util.Base64;
import java.util.Set;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * HTTP filter that implements idempotency key checking for POST/PUT/PATCH requests.
 * <p>
 * Uses Redis to store cached responses, enabling idempotency across multiple
 * application instances. When a request includes an X-Idempotency-Key header,
 * the filter caches the response in Redis with a configurable TTL.
 * </p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyFilter.class);
    private static final String IDEMPOTENCY_KEY_HEADER = "X-Idempotency-Key";
    private static final String REDIS_KEY_PREFIX = "idempotency:";
    private static final String FIELD_SEPARATOR = "|||";
    private static final Set<String> NON_IDEMPOTENT_METHODS = Set.of(
            HttpMethod.POST.name(),
            HttpMethod.PUT.name(),
            HttpMethod.PATCH.name()
    );

    private final StringRedisTemplate redisTemplate;

    @Value("${photoblast.idempotency.ttl-minutes:60}")
    private long ttlMinutes;

    public IdempotencyFilter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (isIdempotent(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String idempotencyKey = request.getHeader(IDEMPOTENCY_KEY_HEADER);

        if (isNull(idempotencyKey) || idempotencyKey.isBlank()) {
            log.warn("Missing required {} header for {} {}",
                    IDEMPOTENCY_KEY_HEADER, request.getMethod(), request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Missing required header: " + IDEMPOTENCY_KEY_HEADER + "\"}");
            return;
        }

        String redisKey = REDIS_KEY_PREFIX + idempotencyKey;

        String cached = redisTemplate.opsForValue().get(redisKey);
        if (nonNull(cached)) {
            log.info("Returning cached response for idempotency key: {}", idempotencyKey);
            writeCachedResponse(response, cached);
            return;
        }

        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        filterChain.doFilter(request, responseWrapper);
        cacheResponse(redisKey, responseWrapper);
        responseWrapper.copyBodyToResponse();
    }

    private boolean isIdempotent(HttpServletRequest request) {
        return !NON_IDEMPOTENT_METHODS.contains(request.getMethod());
    }

    private void writeCachedResponse(HttpServletResponse response, String cached) throws IOException {
        String[] parts = cached.split("\\|\\|\\|", 3);
        if (parts.length < 3) {
            log.warn("Invalid cached response format");
            return;
        }

        int status = Integer.parseInt(parts[0]);
        String contentType = parts[1];
        byte[] body = Base64.getDecoder().decode(parts[2]);

        response.setStatus(status);
        if (!contentType.isEmpty()) {
            response.setContentType(contentType);
        }
        if (body.length > 0) {
            response.getOutputStream().write(body);
        }
    }

    private void cacheResponse(String redisKey, ContentCachingResponseWrapper responseWrapper) {
        int status = responseWrapper.getStatus();
        String contentType = nonNull(responseWrapper.getContentType()) ? responseWrapper.getContentType() : "";
        String bodyBase64 = Base64.getEncoder().encodeToString(responseWrapper.getContentAsByteArray());

        String value = status + FIELD_SEPARATOR + contentType + FIELD_SEPARATOR + bodyBase64;

        redisTemplate.opsForValue().set(redisKey, value, Duration.ofMinutes(ttlMinutes));
        log.info("Cached response in Redis: key={}, status={}, ttl={}min", redisKey, status, ttlMinutes);
    }
}

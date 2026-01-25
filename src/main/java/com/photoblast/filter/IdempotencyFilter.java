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
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HTTP filter that implements idempotency key checking for POST requests.
 * <p>
 * When a request includes an X-Idempotency-Key header, the filter caches the response.
 * Subsequent requests with the same key return the cached response without
 * re-executing the request handler.
 * </p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyFilter.class);
    private static final String IDEMPOTENCY_KEY_HEADER = "X-Idempotency-Key";

    private final Map<String, CachedResponse> cache = new ConcurrentHashMap<>();

    @Value("${photoblast.idempotency.ttl-minutes:60}")
    private long ttlMinutes;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String idempotencyKey = request.getHeader(IDEMPOTENCY_KEY_HEADER);

        if (idempotencyKey == null || idempotencyKey.isBlank() || !isIdempotentMethod(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        cleanupExpiredEntries();

        CachedResponse cached = cache.get(idempotencyKey);
        if (cached != null && !cached.isExpired()) {
            log.info("Returning cached response for idempotency key: {}", idempotencyKey);
            writeCachedResponse(response, cached);
            return;
        }

        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        filterChain.doFilter(request, responseWrapper);

        cacheResponse(idempotencyKey, responseWrapper);

        responseWrapper.copyBodyToResponse();
    }

    private boolean isIdempotentMethod(HttpServletRequest request) {
        String method = request.getMethod();
        return "POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method);
    }

    private void writeCachedResponse(HttpServletResponse response, CachedResponse cached) throws IOException {
        response.setStatus(cached.status());
        response.setContentType(cached.contentType());
        if (cached.body() != null && cached.body().length > 0) {
            response.getOutputStream().write(cached.body());
        }
    }

    private void cacheResponse(String key, ContentCachingResponseWrapper responseWrapper) {
        long expiresAt = Instant.now().plusSeconds(ttlMinutes * 60).toEpochMilli();
        CachedResponse cached = new CachedResponse(
                responseWrapper.getStatus(),
                responseWrapper.getContentType(),
                responseWrapper.getContentAsByteArray(),
                expiresAt
        );
        cache.put(key, cached);
        log.info("Cached response for idempotency key: {}, status: {}, expiresIn: {}min",
                key, cached.status(), ttlMinutes);
    }

    private void cleanupExpiredEntries() {
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private record CachedResponse(int status, String contentType, byte[] body, long expiresAt) {
        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}

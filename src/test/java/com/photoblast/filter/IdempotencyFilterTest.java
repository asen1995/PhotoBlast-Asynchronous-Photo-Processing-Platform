package com.photoblast.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("IdempotencyFilter Unit Tests")
class IdempotencyFilterTest {

    private static final String IDEMPOTENCY_KEY_HEADER = "X-Idempotency-Key";
    private static final String TEST_IDEMPOTENCY_KEY = "test-key-123";

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private FilterChain filterChain;

    private IdempotencyFilter idempotencyFilter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        idempotencyFilter = new IdempotencyFilter(redisTemplate);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Nested
    @DisplayName("Idempotent Methods (GET, DELETE, HEAD, OPTIONS)")
    class IdempotentMethods {

        @ParameterizedTest
        @ValueSource(strings = {"GET", "DELETE", "HEAD", "OPTIONS"})
        @DisplayName("should pass through without idempotency key check")
        void shouldPassThroughForIdempotentMethods(String method) throws ServletException, IOException {
            request.setMethod(method);
            request.setRequestURI("/photos/health");

            idempotencyFilter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verify(redisTemplate, never()).opsForValue();
        }
    }

    @Nested
    @DisplayName("Non-Idempotent Methods (POST, PUT, PATCH)")
    class NonIdempotentMethods {

        @ParameterizedTest
        @ValueSource(strings = {"POST", "PUT", "PATCH"})
        @DisplayName("should return 400 when idempotency key is missing")
        void shouldReturnBadRequestWhenKeyMissing(String method) throws ServletException, IOException {
            request.setMethod(method);
            request.setRequestURI("/photos/upload");

            idempotencyFilter.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(400);
            assertThat(response.getContentAsString()).contains("Missing required header");
            verify(filterChain, never()).doFilter(any(), any());
        }

        @ParameterizedTest
        @ValueSource(strings = {"POST", "PUT", "PATCH"})
        @DisplayName("should return 400 when idempotency key is blank")
        void shouldReturnBadRequestWhenKeyBlank(String method) throws ServletException, IOException {
            request.setMethod(method);
            request.setRequestURI("/photos/upload");
            request.addHeader(IDEMPOTENCY_KEY_HEADER, "   ");

            idempotencyFilter.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(400);
            assertThat(response.getContentAsString()).contains("Missing required header");
        }

        @Test
        @DisplayName("should return cached response when key exists in Redis")
        void shouldReturnCachedResponseWhenKeyExists() throws ServletException, IOException {
            request.setMethod("POST");
            request.setRequestURI("/photos/upload");
            request.addHeader(IDEMPOTENCY_KEY_HEADER, TEST_IDEMPOTENCY_KEY);

            String cachedBody = "{\"success\":true}";
            String cachedValue = "200|||application/json|||" + Base64.getEncoder().encodeToString(cachedBody.getBytes());

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("idempotency:" + TEST_IDEMPOTENCY_KEY)).thenReturn(cachedValue);

            idempotencyFilter.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getContentType()).isEqualTo("application/json");
            assertThat(response.getContentAsString()).isEqualTo(cachedBody);
            verify(filterChain, never()).doFilter(any(), any());
        }

        @Test
        @DisplayName("should process request and cache response when key is new")
        void shouldProcessAndCacheResponseWhenKeyIsNew() throws ServletException, IOException {
            request.setMethod("POST");
            request.setRequestURI("/photos/upload");
            request.addHeader(IDEMPOTENCY_KEY_HEADER, TEST_IDEMPOTENCY_KEY);

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("idempotency:" + TEST_IDEMPOTENCY_KEY)).thenReturn(null);

            idempotencyFilter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(eq(request), any());
            verify(valueOperations).set(eq("idempotency:" + TEST_IDEMPOTENCY_KEY), anyString(), any(Duration.class));
        }
    }

    @Nested
    @DisplayName("Cached Response Parsing")
    class CachedResponseParsing {

        @Test
        @DisplayName("should handle cached response with empty content type")
        void shouldHandleEmptyContentType() throws ServletException, IOException {
            request.setMethod("POST");
            request.setRequestURI("/photos/upload");
            request.addHeader(IDEMPOTENCY_KEY_HEADER, TEST_IDEMPOTENCY_KEY);

            String cachedBody = "OK";
            String cachedValue = "200||||||" + Base64.getEncoder().encodeToString(cachedBody.getBytes());

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("idempotency:" + TEST_IDEMPOTENCY_KEY)).thenReturn(cachedValue);

            idempotencyFilter.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(200);
        }

        @Test
        @DisplayName("should handle cached response with empty body")
        void shouldHandleEmptyBody() throws ServletException, IOException {
            request.setMethod("POST");
            request.setRequestURI("/photos/upload");
            request.addHeader(IDEMPOTENCY_KEY_HEADER, TEST_IDEMPOTENCY_KEY);

            String cachedValue = "204|||application/json|||" + Base64.getEncoder().encodeToString(new byte[0]);

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("idempotency:" + TEST_IDEMPOTENCY_KEY)).thenReturn(cachedValue);

            idempotencyFilter.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(204);
            assertThat(response.getContentAsString()).isEmpty();
        }
    }
}

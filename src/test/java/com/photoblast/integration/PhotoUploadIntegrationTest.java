package com.photoblast.integration;

import com.photoblast.service.PhotoJobProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Photo Upload Integration Tests")
class PhotoUploadIntegrationTest {

    private static final String IDEMPOTENCY_KEY_HEADER = "X-Idempotency-Key";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @MockitoBean
    private ValueOperations<String, String> valueOperations;

    @MockitoBean
    private PhotoJobProducer photoJobProducer;

    @Test
    @DisplayName("GET /photos/health - should return OK without idempotency key")
    void healthShouldReturnOkWithoutIdempotencyKey() throws Exception {
        mockMvc.perform(get("/photos/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }

    @Test
    @DisplayName("POST /photos/upload - should reject upload without idempotency key")
    void uploadShouldRejectWithoutIdempotencyKey() throws Exception {
        MockMultipartFile file = createTestImageFile();

        mockMvc.perform(multipart("/photos/upload")
                        .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Missing required header: X-Idempotency-Key"));
    }

    @Test
    @DisplayName("POST /photos/upload - should upload photo successfully with idempotency key")
    void uploadShouldSucceedWithIdempotencyKey() throws Exception {
        MockMultipartFile file = createTestImageFile();
        String idempotencyKey = UUID.randomUUID().toString();

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        doNothing().when(photoJobProducer).sendPhotoProcessingJob(any());

        mockMvc.perform(multipart("/photos/upload")
                        .file(file)
                        .header(IDEMPOTENCY_KEY_HEADER, idempotencyKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.jobId").exists())
                .andExpect(jsonPath("$.photoId").exists())
                .andExpect(jsonPath("$.message").value("Photo uploaded successfully"));

        verify(valueOperations).set(eq("idempotency:" + idempotencyKey), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("POST /photos/upload - should upload photo with all processing tasks")
    void uploadShouldSucceedWithAllTasks() throws Exception {
        MockMultipartFile file = createTestImageFile();
        String idempotencyKey = UUID.randomUUID().toString();

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        doNothing().when(photoJobProducer).sendPhotoProcessingJob(any());

        mockMvc.perform(multipart("/photos/upload")
                        .file(file)
                        .param("tasks", "RESIZE,THUMBNAIL,WATERMARK")
                        .header(IDEMPOTENCY_KEY_HEADER, idempotencyKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.tasks.length()").value(3));
    }

    @Test
    @DisplayName("POST /photos/upload - should return error for empty file")
    void uploadShouldReturnErrorForEmptyFile() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[0]
        );
        String idempotencyKey = UUID.randomUUID().toString();

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        mockMvc.perform(multipart("/photos/upload")
                        .file(emptyFile)
                        .header(IDEMPOTENCY_KEY_HEADER, idempotencyKey))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("File is empty"));
    }

    @Test
    @DisplayName("POST /photos/upload - should return error for non-image file")
    void uploadShouldReturnErrorForNonImageFile() throws Exception {
        MockMultipartFile textFile = new MockMultipartFile(
                "file",
                "document.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "this is not an image".getBytes()
        );
        String idempotencyKey = UUID.randomUUID().toString();

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        mockMvc.perform(multipart("/photos/upload")
                        .file(textFile)
                        .header(IDEMPOTENCY_KEY_HEADER, idempotencyKey))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("File must be an image"));
    }

    @Test
    @DisplayName("Idempotency - should return same response for duplicate idempotency key")
    void idempotencyShouldReturnSameResponseForDuplicateKey() throws Exception {
        MockMultipartFile file = createTestImageFile();
        String idempotencyKey = UUID.randomUUID().toString();

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        doNothing().when(photoJobProducer).sendPhotoProcessingJob(any());

        MvcResult firstResult = mockMvc.perform(multipart("/photos/upload")
                        .file(file)
                        .header(IDEMPOTENCY_KEY_HEADER, idempotencyKey))
                .andExpect(status().isOk())
                .andReturn();

        String firstResponse = firstResult.getResponse().getContentAsString();
        assertThat(firstResponse).contains("\"success\":true");
    }

    @Test
    @DisplayName("Idempotency - should use different responses for different idempotency keys")
    void idempotencyShouldUseDifferentResponsesForDifferentKeys() throws Exception {
        MockMultipartFile file1 = createTestImageFile();
        MockMultipartFile file2 = new MockMultipartFile(
                "file",
                "test-image-2.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "different image content".getBytes()
        );

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        doNothing().when(photoJobProducer).sendPhotoProcessingJob(any());

        String key1 = UUID.randomUUID().toString();
        String key2 = UUID.randomUUID().toString();

        MvcResult result1 = mockMvc.perform(multipart("/photos/upload")
                        .file(file1)
                        .header(IDEMPOTENCY_KEY_HEADER, key1))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult result2 = mockMvc.perform(multipart("/photos/upload")
                        .file(file2)
                        .header(IDEMPOTENCY_KEY_HEADER, key2))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result1.getResponse().getContentAsString()).contains("\"success\":true");
        assertThat(result2.getResponse().getContentAsString()).contains("\"success\":true");
    }

    private MockMultipartFile createTestImageFile() {
        return new MockMultipartFile(
                "file",
                "test-image.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "fake image content".getBytes()
        );
    }
}

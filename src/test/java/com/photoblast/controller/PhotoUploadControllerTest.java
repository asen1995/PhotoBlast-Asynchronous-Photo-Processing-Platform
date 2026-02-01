package com.photoblast.controller;

import com.photoblast.dto.PhotoUploadResponse;
import com.photoblast.enums.ProcessingTask;
import com.photoblast.filter.IdempotencyFilter;
import com.photoblast.service.PhotoUploadService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PhotoUploadController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = IdempotencyFilter.class))
@DisplayName("PhotoUploadController Unit Tests")
class PhotoUploadControllerTest {

    private static final String JOB_ID = "job-123";
    private static final String PHOTO_ID = "photo-456";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PhotoUploadService photoUploadService;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @Test
    @DisplayName("GET /photos/health - should return OK status")
    void healthShouldReturnOkStatus() throws Exception {
        mockMvc.perform(get("/photos/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }

    @Test
    @DisplayName("POST /photos/upload - should upload photo successfully with default tasks")
    void uploadShouldSucceedWithDefaultTasks() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-image.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "test image content".getBytes()
        );

        List<ProcessingTask> expectedTasks = List.of(ProcessingTask.RESIZE, ProcessingTask.THUMBNAIL);
        PhotoUploadResponse successResponse = PhotoUploadResponse.success(JOB_ID, PHOTO_ID, expectedTasks);

        when(photoUploadService.uploadPhoto(any(), anyList())).thenReturn(successResponse);

        mockMvc.perform(multipart("/photos/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.jobId").value(JOB_ID))
                .andExpect(jsonPath("$.photoId").value(PHOTO_ID))
                .andExpect(jsonPath("$.message").value("Photo uploaded successfully"));

        verify(photoUploadService).uploadPhoto(any(), anyList());
    }

    @Test
    @DisplayName("POST /photos/upload - should upload photo with custom tasks")
    void uploadShouldSucceedWithCustomTasks() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-image.png",
                MediaType.IMAGE_PNG_VALUE,
                "test image content".getBytes()
        );

        List<ProcessingTask> customTasks = List.of(ProcessingTask.RESIZE, ProcessingTask.WATERMARK);
        PhotoUploadResponse successResponse = PhotoUploadResponse.success(JOB_ID, PHOTO_ID, customTasks);

        when(photoUploadService.uploadPhoto(any(), anyList())).thenReturn(successResponse);

        mockMvc.perform(multipart("/photos/upload")
                        .file(file)
                        .param("tasks", "RESIZE,WATERMARK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.tasks[0]").value("RESIZE"))
                .andExpect(jsonPath("$.tasks[1]").value("WATERMARK"));
    }

    @Test
    @DisplayName("POST /photos/upload - should return bad request when file is empty")
    void uploadShouldReturnBadRequestWhenFileIsEmpty() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[0]
        );

        PhotoUploadResponse errorResponse = PhotoUploadResponse.error("File is empty");

        when(photoUploadService.uploadPhoto(any(), anyList())).thenReturn(errorResponse);

        mockMvc.perform(multipart("/photos/upload")
                        .file(emptyFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("File is empty"));
    }

    @Test
    @DisplayName("POST /photos/upload - should return bad request when file is not an image")
    void uploadShouldReturnBadRequestWhenFileIsNotImage() throws Exception {
        MockMultipartFile textFile = new MockMultipartFile(
                "file",
                "document.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "this is not an image".getBytes()
        );

        PhotoUploadResponse errorResponse = PhotoUploadResponse.error("File must be an image");

        when(photoUploadService.uploadPhoto(any(), anyList())).thenReturn(errorResponse);

        mockMvc.perform(multipart("/photos/upload")
                        .file(textFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("File must be an image"));
    }

    @Test
    @DisplayName("POST /photos/upload - should return bad request when file parameter is missing")
    void uploadShouldReturnBadRequestWhenFileMissing() throws Exception {
        mockMvc.perform(multipart("/photos/upload"))
                .andExpect(status().isBadRequest());

        verify(photoUploadService, never()).uploadPhoto(any(), anyList());
    }
}

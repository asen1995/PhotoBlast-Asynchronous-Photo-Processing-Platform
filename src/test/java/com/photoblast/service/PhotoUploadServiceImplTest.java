package com.photoblast.service;

import com.photoblast.dto.PhotoUploadResponse;
import com.photoblast.enums.ProcessingTask;
import com.photoblast.model.PhotoProcessingJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("PhotoUploadServiceImpl Unit Tests")
class PhotoUploadServiceImplTest {

    @Mock
    private PhotoJobProducer photoJobProducer;

    @Captor
    private ArgumentCaptor<PhotoProcessingJob> jobCaptor;

    @TempDir
    Path tempDir;

    private PhotoUploadServiceImpl photoUploadService;

    @BeforeEach
    void setUp() {
        photoUploadService = new PhotoUploadServiceImpl(photoJobProducer);
        ReflectionTestUtils.setField(photoUploadService, "uploadDir", tempDir.toString());
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("should return error when file is empty")
        void shouldReturnErrorWhenFileIsEmpty() {
            MockMultipartFile emptyFile = new MockMultipartFile(
                    "file",
                    "empty.jpg",
                    MediaType.IMAGE_JPEG_VALUE,
                    new byte[0]
            );

            PhotoUploadResponse response = photoUploadService.uploadPhoto(
                    emptyFile,
                    List.of(ProcessingTask.RESIZE)
            );

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getMessage()).isEqualTo("File is empty");
            verify(photoJobProducer, never()).sendPhotoProcessingJob(any());
        }

        @Test
        @DisplayName("should return error when content type is null")
        void shouldReturnErrorWhenContentTypeIsNull() {
            MockMultipartFile fileWithNullContentType = new MockMultipartFile(
                    "file",
                    "test.jpg",
                    null,
                    "content".getBytes()
            );

            PhotoUploadResponse response = photoUploadService.uploadPhoto(
                    fileWithNullContentType,
                    List.of(ProcessingTask.RESIZE)
            );

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getMessage()).isEqualTo("File must be an image");
            verify(photoJobProducer, never()).sendPhotoProcessingJob(any());
        }

        @Test
        @DisplayName("should return error when file is not an image")
        void shouldReturnErrorWhenFileIsNotImage() {
            MockMultipartFile textFile = new MockMultipartFile(
                    "file",
                    "document.txt",
                    MediaType.TEXT_PLAIN_VALUE,
                    "this is not an image".getBytes()
            );

            PhotoUploadResponse response = photoUploadService.uploadPhoto(
                    textFile,
                    List.of(ProcessingTask.RESIZE)
            );

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getMessage()).isEqualTo("File must be an image");
            verify(photoJobProducer, never()).sendPhotoProcessingJob(any());
        }

        @Test
        @DisplayName("should return error for application/pdf content type")
        void shouldReturnErrorForPdfContentType() {
            MockMultipartFile pdfFile = new MockMultipartFile(
                    "file",
                    "document.pdf",
                    "application/pdf",
                    "pdf content".getBytes()
            );

            PhotoUploadResponse response = photoUploadService.uploadPhoto(
                    pdfFile,
                    List.of(ProcessingTask.RESIZE)
            );

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getMessage()).isEqualTo("File must be an image");
        }
    }

    @Nested
    @DisplayName("Successful Upload Tests")
    class SuccessfulUploadTests {

        @Test
        @DisplayName("should upload JPEG image successfully")
        void shouldUploadJpegImageSuccessfully() {
            MockMultipartFile jpegFile = new MockMultipartFile(
                    "file",
                    "photo.jpg",
                    MediaType.IMAGE_JPEG_VALUE,
                    "fake jpeg content".getBytes()
            );
            List<ProcessingTask> tasks = List.of(ProcessingTask.RESIZE, ProcessingTask.THUMBNAIL);

            PhotoUploadResponse response = photoUploadService.uploadPhoto(jpegFile, tasks);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getMessage()).isEqualTo("Photo uploaded successfully");
            assertThat(response.getJobId()).isNotNull();
            assertThat(response.getPhotoId()).isNotNull();
            assertThat(response.getTasks()).isEqualTo(tasks);

            verify(photoJobProducer).sendPhotoProcessingJob(jobCaptor.capture());
            PhotoProcessingJob capturedJob = jobCaptor.getValue();
            assertThat(capturedJob.getPhotoId()).isEqualTo(response.getPhotoId());
            assertThat(capturedJob.getTasks()).isEqualTo(tasks);
        }

        @Test
        @DisplayName("should upload PNG image successfully")
        void shouldUploadPngImageSuccessfully() {
            MockMultipartFile pngFile = new MockMultipartFile(
                    "file",
                    "photo.png",
                    MediaType.IMAGE_PNG_VALUE,
                    "fake png content".getBytes()
            );
            List<ProcessingTask> tasks = List.of(ProcessingTask.WATERMARK);

            PhotoUploadResponse response = photoUploadService.uploadPhoto(pngFile, tasks);

            assertThat(response.isSuccess()).isTrue();
            verify(photoJobProducer).sendPhotoProcessingJob(any());
        }

        @Test
        @DisplayName("should upload GIF image successfully")
        void shouldUploadGifImageSuccessfully() {
            MockMultipartFile gifFile = new MockMultipartFile(
                    "file",
                    "animation.gif",
                    MediaType.IMAGE_GIF_VALUE,
                    "fake gif content".getBytes()
            );

            PhotoUploadResponse response = photoUploadService.uploadPhoto(
                    gifFile,
                    List.of(ProcessingTask.THUMBNAIL)
            );

            assertThat(response.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("should store file with correct extension")
        void shouldStoreFileWithCorrectExtension() {
            MockMultipartFile jpegFile = new MockMultipartFile(
                    "file",
                    "my-photo.jpeg",
                    MediaType.IMAGE_JPEG_VALUE,
                    "fake jpeg content".getBytes()
            );

            PhotoUploadResponse response = photoUploadService.uploadPhoto(
                    jpegFile,
                    List.of(ProcessingTask.RESIZE)
            );

            assertThat(response.isSuccess()).isTrue();

            verify(photoJobProducer).sendPhotoProcessingJob(jobCaptor.capture());
            PhotoProcessingJob capturedJob = jobCaptor.getValue();
            assertThat(capturedJob.getOriginalPath()).endsWith(".jpeg");
        }

        @Test
        @DisplayName("should handle file without extension")
        void shouldHandleFileWithoutExtension() {
            MockMultipartFile fileWithoutExtension = new MockMultipartFile(
                    "file",
                    "photo",
                    MediaType.IMAGE_JPEG_VALUE,
                    "fake image content".getBytes()
            );

            PhotoUploadResponse response = photoUploadService.uploadPhoto(
                    fileWithoutExtension,
                    List.of(ProcessingTask.RESIZE)
            );

            assertThat(response.isSuccess()).isTrue();
            verify(photoJobProducer).sendPhotoProcessingJob(jobCaptor.capture());
            // Should default to .jpg extension
            assertThat(jobCaptor.getValue().getOriginalPath()).endsWith(".jpg");
        }
    }

    @Nested
    @DisplayName("Processing Tasks Tests")
    class ProcessingTasksTests {

        @Test
        @DisplayName("should send job with single task")
        void shouldSendJobWithSingleTask() {
            MockMultipartFile file = createTestImageFile();
            List<ProcessingTask> tasks = List.of(ProcessingTask.RESIZE);

            photoUploadService.uploadPhoto(file, tasks);

            verify(photoJobProducer).sendPhotoProcessingJob(jobCaptor.capture());
            assertThat(jobCaptor.getValue().getTasks()).hasSize(1);
            assertThat(jobCaptor.getValue().getTasks()).containsExactly(ProcessingTask.RESIZE);
        }

        @Test
        @DisplayName("should send job with multiple tasks")
        void shouldSendJobWithMultipleTasks() {
            MockMultipartFile file = createTestImageFile();
            List<ProcessingTask> tasks = List.of(
                    ProcessingTask.RESIZE,
                    ProcessingTask.THUMBNAIL,
                    ProcessingTask.WATERMARK
            );

            photoUploadService.uploadPhoto(file, tasks);

            verify(photoJobProducer).sendPhotoProcessingJob(jobCaptor.capture());
            assertThat(jobCaptor.getValue().getTasks()).hasSize(3);
            assertThat(jobCaptor.getValue().getTasks()).containsExactlyElementsOf(tasks);
        }
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

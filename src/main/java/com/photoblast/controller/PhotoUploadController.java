package com.photoblast.controller;

import com.photoblast.dto.PhotoUploadResponse;
import com.photoblast.model.PhotoProcessingJob.ProcessingTask;
import com.photoblast.service.PhotoUploadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST controller for photo upload operations.
 * <p>
 * Provides endpoints for uploading photos and health checks.
 * </p>
 */
@RestController
@RequestMapping("/photos")
public class PhotoUploadController {

    private final PhotoUploadService photoUploadService;

    /**
     * Constructs a new PhotoUploadController with the given service.
     *
     * @param photoUploadService the photo upload service
     */
    public PhotoUploadController(PhotoUploadService photoUploadService) {
        this.photoUploadService = photoUploadService;
    }

    /**
     * Uploads a photo and queues it for processing.
     *
     * @param file  the image file to upload
     * @param tasks the processing tasks to perform
     * @return response containing job details or error message
     */
    @PostMapping("/upload")
    public ResponseEntity<PhotoUploadResponse> uploadPhoto(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "tasks", defaultValue = "RESIZE,THUMBNAIL") List<ProcessingTask> tasks) {

        PhotoUploadResponse response = photoUploadService.uploadPhoto(file, tasks);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Health check endpoint.
     *
     * @return OK status if service is running
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}

package com.photoblast.controller;

import com.photoblast.dto.PhotoUploadResponse;
import com.photoblast.model.PhotoProcessingJob.ProcessingTask;
import com.photoblast.service.PhotoUploadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/photos")
public class PhotoUploadController {

    private final PhotoUploadService photoUploadService;

    public PhotoUploadController(PhotoUploadService photoUploadService) {
        this.photoUploadService = photoUploadService;
    }

    @PostMapping("/upload")
    public ResponseEntity<PhotoUploadResponse> uploadPhoto(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "tasks", defaultValue = "RESIZE,THUMBNAIL") List<ProcessingTask> tasks) {

        PhotoUploadResponse response = photoUploadService.uploadPhoto(file, tasks);

        if (response.success()) {
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.badRequest().body(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}

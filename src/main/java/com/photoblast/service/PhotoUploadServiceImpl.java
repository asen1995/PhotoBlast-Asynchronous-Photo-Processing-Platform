package com.photoblast.service;

import com.photoblast.dto.PhotoUploadResponse;
import com.photoblast.model.PhotoProcessingJob;
import com.photoblast.model.PhotoProcessingJob.ProcessingTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class PhotoUploadServiceImpl implements PhotoUploadService {

    private static final Logger log = LoggerFactory.getLogger(PhotoUploadServiceImpl.class);

    private final PhotoJobProducer photoJobProducer;

    @Value("${photoblast.storage.upload-dir:uploads}")
    private String uploadDir;

    public PhotoUploadServiceImpl(PhotoJobProducer photoJobProducer) {
        this.photoJobProducer = photoJobProducer;
    }

    @Override
    public PhotoUploadResponse uploadPhoto(MultipartFile file, List<ProcessingTask> tasks) {
        if (file.isEmpty()) {
            return PhotoUploadResponse.error("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return PhotoUploadResponse.error("File must be an image");
        }

        try {
            String photoId = UUID.randomUUID().toString();
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String storedFilename = photoId + extension;

            Path uploadPath = Paths.get(uploadDir);
            Files.createDirectories(uploadPath);

            Path filePath = uploadPath.resolve(storedFilename);
            file.transferTo(filePath);

            log.info("Photo uploaded: photoId={}, path={}", photoId, filePath);

            PhotoProcessingJob job = PhotoProcessingJob.create(photoId, filePath.toString(), tasks);
            photoJobProducer.sendPhotoProcessingJob(job);

            return PhotoUploadResponse.success(job.jobId(), photoId, tasks);

        } catch (IOException e) {
            log.error("Failed to upload photo", e);
            return PhotoUploadResponse.error("Failed to store photo: " + e.getMessage());
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}

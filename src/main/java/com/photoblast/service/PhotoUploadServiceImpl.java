package com.photoblast.service;

import com.photoblast.dto.PhotoUploadResponse;
import com.photoblast.model.PhotoProcessingJob;
import com.photoblast.enums.ProcessingTask;
import com.photoblast.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.isNull;

/**
 * Implementation of {@link PhotoUploadService} for handling photo uploads.
 * <p>
 * Validates, stores uploaded photos, and publishes processing jobs to RabbitMQ.
 * </p>
 */
@Service
public class PhotoUploadServiceImpl implements PhotoUploadService {

    private static final Logger log = LoggerFactory.getLogger(PhotoUploadServiceImpl.class);

    private final PhotoJobProducer photoJobProducer;

    @Value("${photoblast.storage.upload-dir:uploads}")
    private String uploadDir;

    /**
     * Constructs a new PhotoUploadServiceImpl with the given producer.
     *
     * @param photoJobProducer the producer for sending processing jobs
     */
    public PhotoUploadServiceImpl(PhotoJobProducer photoJobProducer) {
        this.photoJobProducer = photoJobProducer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PhotoUploadResponse uploadPhoto(MultipartFile file, List<ProcessingTask> tasks) {
        if (file.isEmpty()) {
            return PhotoUploadResponse.error("File is empty");
        }

        String contentType = file.getContentType();
        if (isNull(contentType) || !contentType.startsWith("image/")) {
            return PhotoUploadResponse.error("File must be an image");
        }

        try {
            String photoId = UUID.randomUUID().toString();
            String originalFilename = file.getOriginalFilename();
            String extension = FileUtils.getExtension(originalFilename);
            String storedFilename = photoId + extension;

            Path uploadPath = FileUtils.ensureDirectoryExists(uploadDir);
            Path filePath = uploadPath.resolve(storedFilename);
            file.transferTo(filePath);

            log.info("Photo uploaded: photoId={}, path={}", photoId, filePath);

            PhotoProcessingJob job = PhotoProcessingJob.create(photoId, filePath.toString(), tasks);
            photoJobProducer.sendPhotoProcessingJob(job);

            return PhotoUploadResponse.success(job.getJobId(), photoId, tasks);

        } catch (IOException e) {
            log.error("Failed to upload photo", e);
            return PhotoUploadResponse.error("Failed to store photo: " + e.getMessage());
        }
    }
}

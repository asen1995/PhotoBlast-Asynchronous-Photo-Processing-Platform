package com.photoblast.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Represents a photo processing job message sent through RabbitMQ.
 * <p>
 * Contains all information needed to process an uploaded photo,
 * including the photo location and the list of processing tasks to perform.
 * </p>
 *
 * @param jobId        unique identifier for this processing job
 * @param photoId      unique identifier for the photo being processed
 * @param originalPath file path to the original uploaded photo
 * @param tasks        list of processing tasks to perform on the photo
 * @param createdAt    timestamp when the job was created
 */
public record PhotoProcessingJob(
        String jobId,
        String photoId,
        String originalPath,
        List<ProcessingTask> tasks,
        Instant createdAt
) implements Serializable {

    /**
     * Available processing tasks that can be performed on photos.
     */
    public enum ProcessingTask {
        /** Resize the photo to configured dimensions */
        RESIZE,
        /** Apply a watermark to the photo */
        WATERMARK,
        /** Generate a thumbnail version of the photo */
        THUMBNAIL
    }

    /**
     * Factory method to create a new photo processing job.
     *
     * @param photoId      unique identifier for the photo
     * @param originalPath file path to the original photo
     * @param tasks        list of processing tasks to perform
     * @return new PhotoProcessingJob with generated jobId and current timestamp
     */
    public static PhotoProcessingJob create(String photoId, String originalPath, List<ProcessingTask> tasks) {
        return new PhotoProcessingJob(
                UUID.randomUUID().toString(),
                photoId,
                originalPath,
                tasks,
                Instant.now()
        );
    }
}

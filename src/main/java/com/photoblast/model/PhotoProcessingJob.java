package com.photoblast.model;

import com.photoblast.enums.ProcessingTask;
import lombok.Value;

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
 */
@Value
public class PhotoProcessingJob implements Serializable {

    /** Unique identifier for this processing job */
    String jobId;

    /** Unique identifier for the photo being processed */
    String photoId;

    /** File path to the original uploaded photo */
    String originalPath;

    /** List of processing tasks to perform on the photo */
    List<ProcessingTask> tasks;

    /** Timestamp when the job was created */
    Instant createdAt;

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

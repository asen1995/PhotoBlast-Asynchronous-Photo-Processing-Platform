package com.photoblast.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PhotoProcessingJob(
        String jobId,
        String photoId,
        String originalPath,
        List<ProcessingTask> tasks,
        Instant createdAt
) implements Serializable {

    public enum ProcessingTask {
        RESIZE,
        WATERMARK,
        THUMBNAIL
    }

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

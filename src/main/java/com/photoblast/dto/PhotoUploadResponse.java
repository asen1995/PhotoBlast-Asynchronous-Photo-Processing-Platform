package com.photoblast.dto;

import com.photoblast.model.PhotoProcessingJob.ProcessingTask;

import java.util.List;

public record PhotoUploadResponse(
        boolean success,
        String message,
        String jobId,
        String photoId,
        List<ProcessingTask> tasks
) {

    public static PhotoUploadResponse success(String jobId, String photoId, List<ProcessingTask> tasks) {
        return new PhotoUploadResponse(true, "Photo uploaded successfully", jobId, photoId, tasks);
    }

    public static PhotoUploadResponse error(String message) {
        return new PhotoUploadResponse(false, message, null, null, null);
    }
}

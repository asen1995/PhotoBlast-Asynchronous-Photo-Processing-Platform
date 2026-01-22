package com.photoblast.service;

import com.photoblast.model.PhotoProcessingJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class PhotoJobConsumer {

    private static final Logger log = LoggerFactory.getLogger(PhotoJobConsumer.class);

    @RabbitListener(queues = "${photoblast.rabbitmq.queue.photo-process}")
    public void processPhotoJob(PhotoProcessingJob job) {
        log.info("Received photo processing job: jobId={}, photoId={}", job.jobId(), job.photoId());

        for (PhotoProcessingJob.ProcessingTask task : job.tasks()) {
            processTask(job, task);
        }

        log.info("Completed photo processing job: jobId={}", job.jobId());
    }

    private void processTask(PhotoProcessingJob job, PhotoProcessingJob.ProcessingTask task) {
        log.info("Processing task {} for photo: photoId={}", task, job.photoId());

        switch (task) {
            case RESIZE -> handleResize(job);
            case WATERMARK -> handleWatermark(job);
            case THUMBNAIL -> handleThumbnail(job);
        }

        log.info("Completed task {} for photo: photoId={}", task, job.photoId());
    }

    private void handleResize(PhotoProcessingJob job) {
        // TODO: Implement actual resize logic
        log.info("Resizing photo: {}", job.originalPath());
    }

    private void handleWatermark(PhotoProcessingJob job) {
        // TODO: Implement actual watermark logic
        log.info("Adding watermark to photo: {}", job.originalPath());
    }

    private void handleThumbnail(PhotoProcessingJob job) {
        // TODO: Implement actual thumbnail generation logic
        log.info("Generating thumbnail for photo: {}", job.originalPath());
    }
}

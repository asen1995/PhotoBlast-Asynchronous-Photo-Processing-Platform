package com.photoblast.service;

import com.photoblast.model.PhotoProcessingJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * Consumer service that listens for and processes photo processing jobs from RabbitMQ.
 * <p>
 * Consumes {@link PhotoProcessingJob} messages from the photo processing queue
 * and executes the requested tasks such as resizing, watermarking, and thumbnail generation.
 * </p>
 */
@Service
public class PhotoJobConsumer {

    private static final Logger log = LoggerFactory.getLogger(PhotoJobConsumer.class);

    /**
     * Listens for and processes photo processing jobs from the queue.
     * Executes all tasks defined in the job sequentially.
     *
     * @param job the photo processing job received from RabbitMQ
     */
    @RabbitListener(queues = "${photoblast.rabbitmq.queue.photo-process}")
    public void processPhotoJob(PhotoProcessingJob job) {
        log.info("Received photo processing job: jobId={}, photoId={}", job.jobId(), job.photoId());

        for (PhotoProcessingJob.ProcessingTask task : job.tasks()) {
            processTask(job, task);
        }

        log.info("Completed photo processing job: jobId={}", job.jobId());
    }

    /**
     * Routes and processes a single task for the given job.
     *
     * @param job  the photo processing job
     * @param task the specific task to process
     */
    private void processTask(PhotoProcessingJob job, PhotoProcessingJob.ProcessingTask task) {
        log.info("Processing task {} for photo: photoId={}", task, job.photoId());

        switch (task) {
            case RESIZE -> handleResize(job);
            case WATERMARK -> handleWatermark(job);
            case THUMBNAIL -> handleThumbnail(job);
        }

        log.info("Completed task {} for photo: photoId={}", task, job.photoId());
    }

    /**
     * Handles the resize task for a photo.
     *
     * @param job the photo processing job containing the photo to resize
     */
    private void handleResize(PhotoProcessingJob job) {
        // TODO: Implement actual resize logic
        log.info("Resizing photo: {}", job.originalPath());
    }

    /**
     * Handles the watermark task for a photo.
     *
     * @param job the photo processing job containing the photo to watermark
     */
    private void handleWatermark(PhotoProcessingJob job) {
        // TODO: Implement actual watermark logic
        log.info("Adding watermark to photo: {}", job.originalPath());
    }

    /**
     * Handles the thumbnail generation task for a photo.
     *
     * @param job the photo processing job containing the photo to generate thumbnail for
     */
    private void handleThumbnail(PhotoProcessingJob job) {
        // TODO: Implement actual thumbnail generation logic
        log.info("Generating thumbnail for photo: {}", job.originalPath());
    }
}

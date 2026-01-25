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
 * and delegates to {@link ImageService} for actual image processing operations.
 * </p>
 */
@Service
public class PhotoJobConsumer {

    private static final Logger log = LoggerFactory.getLogger(PhotoJobConsumer.class);

    private final ImageService imageService;

    /**
     * Constructs a new PhotoJobConsumer with the given ImageService.
     *
     * @param imageService the image service for processing operations
     */
    public PhotoJobConsumer(ImageService imageService) {
        this.imageService = imageService;
    }

    /**
     * Listens for and processes photo processing jobs from the queue.
     * Executes all tasks defined in the job sequentially.
     *
     * @param job the photo processing job received from RabbitMQ
     */
    @RabbitListener(queues = "${photoblast.rabbitmq.queue.photo-process}")
    public void processPhotoJob(PhotoProcessingJob job) {
        log.info("Received photo processing job: jobId={}, photoId={}", job.getJobId(), job.getPhotoId());

        for (PhotoProcessingJob.ProcessingTask task : job.getTasks()) {
            processTask(job, task);
        }

        log.info("Completed photo processing job: jobId={}", job.getJobId());
    }

    /**
     * Routes and processes a single task for the given job.
     *
     * @param job  the photo processing job
     * @param task the specific task to process
     */
    private void processTask(PhotoProcessingJob job, PhotoProcessingJob.ProcessingTask task) {
        log.info("Processing task {} for photo: photoId={}", task, job.getPhotoId());

        switch (task) {
            case RESIZE -> imageService.resize(job.getOriginalPath(), job.getPhotoId());
            case WATERMARK -> imageService.watermark(job.getOriginalPath(), job.getPhotoId());
            case THUMBNAIL -> imageService.thumbnail(job.getOriginalPath(), job.getPhotoId());
        }

        log.info("Completed task {} for photo: photoId={}", task, job.getPhotoId());
    }
}

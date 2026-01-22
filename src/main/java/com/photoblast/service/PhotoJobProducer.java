package com.photoblast.service;

import com.photoblast.model.PhotoProcessingJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PhotoJobProducer {

    private static final Logger log = LoggerFactory.getLogger(PhotoJobProducer.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${photoblast.rabbitmq.exchange.photo}")
    private String photoExchange;

    @Value("${photoblast.rabbitmq.routing-key.photo-process}")
    private String photoProcessRoutingKey;

    public PhotoJobProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendPhotoProcessingJob(PhotoProcessingJob job) {
        log.info("Sending photo processing job: jobId={}, photoId={}", job.jobId(), job.photoId());
        rabbitTemplate.convertAndSend(photoExchange, photoProcessRoutingKey, job);
        log.info("Photo processing job sent successfully: jobId={}", job.jobId());
    }
}

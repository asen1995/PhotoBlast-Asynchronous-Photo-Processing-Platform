package com.photoblast.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for the PhotoBlast messaging infrastructure.
 * <p>
 * Configures queues, exchanges, bindings, and message converters for
 * asynchronous photo processing jobs. Includes dead-letter queue setup
 * for failed message handling.
 * </p>
 */
@Configuration
public class RabbitMQConfig {

    @Value("${photoblast.rabbitmq.queue.photo-process}")
    private String photoProcessQueue;

    @Value("${photoblast.rabbitmq.queue.dead-letter}")
    private String deadLetterQueue;

    @Value("${photoblast.rabbitmq.exchange.photo}")
    private String photoExchange;

    @Value("${photoblast.rabbitmq.routing-key.photo-process}")
    private String photoProcessRoutingKey;

    /**
     * Creates the main photo processing queue with dead-letter configuration.
     *
     * @return durable queue for photo processing jobs
     */
    @Bean
    public Queue photoProcessQueue() {
        return QueueBuilder.durable(photoProcessQueue)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", deadLetterQueue)
                .build();
    }

    /**
     * Creates the dead-letter queue for failed photo processing jobs.
     *
     * @return durable dead-letter queue
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(deadLetterQueue).build();
    }

    /**
     * Creates the direct exchange for routing photo processing messages.
     *
     * @return direct exchange for photo processing
     */
    @Bean
    public DirectExchange photoExchange() {
        return new DirectExchange(photoExchange);
    }

    /**
     * Binds the photo processing queue to the exchange with the configured routing key.
     *
     * @param photoProcessQueue the photo processing queue
     * @param photoExchange     the photo exchange
     * @return binding between queue and exchange
     */
    @Bean
    public Binding photoProcessBinding(Queue photoProcessQueue, DirectExchange photoExchange) {
        return BindingBuilder.bind(photoProcessQueue)
                .to(photoExchange)
                .with(photoProcessRoutingKey);
    }

    /**
     * Creates a JSON message converter for serializing job messages.
     *
     * @return Jackson-based JSON message converter
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Creates a configured RabbitTemplate for sending messages.
     *
     * @param connectionFactory the RabbitMQ connection factory
     * @return configured RabbitTemplate with JSON converter
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}

package com.photoblast.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

    @Bean
    public Queue photoProcessQueue() {
        return QueueBuilder.durable(photoProcessQueue)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", deadLetterQueue)
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(deadLetterQueue).build();
    }

    @Bean
    public DirectExchange photoExchange() {
        return new DirectExchange(photoExchange);
    }

    @Bean
    public Binding photoProcessBinding(Queue photoProcessQueue, DirectExchange photoExchange) {
        return BindingBuilder.bind(photoProcessQueue)
                .to(photoExchange)
                .with(photoProcessRoutingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}

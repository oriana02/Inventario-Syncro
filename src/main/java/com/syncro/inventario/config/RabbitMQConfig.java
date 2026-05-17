package com.syncro.inventario.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String INVENTARIO_SINCRONIZAR_QUEUE = "inventario.sincronizar";

    @Bean
    public Queue inventarioSincronizarQueue() {
        return new Queue(INVENTARIO_SINCRONIZAR_QUEUE, true);
    }

    @Bean
    public MessageConverter converter() {
        return new org.springframework.amqp.support.converter.SimpleMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter());
        return template;
    }
}

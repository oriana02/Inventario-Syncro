package com.syncro.inventario.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String COLA_INVENTARIO = "inventario.sincronizar";

    @Bean
    public Queue colaInventario() {
        return new Queue(COLA_INVENTARIO, true);
    }

    // transforma objetos a texto plano formtato JSON
    @Bean
    public MessageConverter converter() {
        return new Jackson2JsonMessageConverter();

    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter());
        return template;
    }
}

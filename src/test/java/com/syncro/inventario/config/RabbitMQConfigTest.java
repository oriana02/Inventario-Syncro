package com.syncro.inventario.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class RabbitMQConfigTest {

    private RabbitMQConfig config;

    @BeforeEach
    void setUp() {
        config = new RabbitMQConfig();
    }

    @Test
    void constanteColaInventario_tieneValorEsperado() {
        assertEquals("inventario.sincronizar", RabbitMQConfig.COLA_INVENTARIO);
    }

    @Test
    void constanteExchange_tieneValorEsperado() {
        assertEquals("pedidos.exchange", RabbitMQConfig.EXCHANGE);
    }

    @Test
    void colaInventario_esCreada_conNombreCorrecto() {
        Queue cola = config.colaInventario();

        assertNotNull(cola);
        assertEquals("inventario.sincronizar", cola.getName());
        assertTrue(cola.isDurable());
    }

    @Test
    void pedidosExchange_esCreado_conNombreCorrecto() {
        FanoutExchange exchange = config.pedidosExchange();

        assertNotNull(exchange);
        assertEquals("pedidos.exchange", exchange.getName());
    }

    @Test
    void bindingInventario_enlazaColaConExchange() {
        Queue cola = config.colaInventario();
        FanoutExchange exchange = config.pedidosExchange();

        Binding binding = config.bindingInventario(cola, exchange);

        assertNotNull(binding);
        assertEquals("inventario.sincronizar", binding.getDestination());
        assertEquals(Binding.DestinationType.QUEUE, binding.getDestinationType());
    }

    @Test
    void converter_retornaJackson2JsonMessageConverter() {
        MessageConverter converter = config.converter();

        assertNotNull(converter);
        assertInstanceOf(Jackson2JsonMessageConverter.class, converter);
    }

    @Test
    void rabbitTemplate_esCreado_conConverterConfigurado() {
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);

        RabbitTemplate template = config.rabbitTemplate(connectionFactory);

        assertNotNull(template);
        assertInstanceOf(Jackson2JsonMessageConverter.class, template.getMessageConverter());
    }
}

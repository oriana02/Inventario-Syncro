package com.syncro.inventario;

import com.syncro.inventario.config.RabbitMQConfig;
import com.syncro.inventario.consumer.PedidoCreadoConsumer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration",
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=syncro-secret-key-para-test-minimo-32-chars!!",
    "jwt.expiration=86400000"
})
class InventarioApplicationTests {

    @MockBean
    ConnectionFactory connectionFactory;

    @MockBean
    RabbitTemplate rabbitTemplate;

    @MockBean
    PedidoCreadoConsumer pedidoCreadoConsumer;

    @Test
    void contextLoads() {
    }
}

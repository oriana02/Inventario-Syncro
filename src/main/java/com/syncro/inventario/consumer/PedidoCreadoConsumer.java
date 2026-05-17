package com.syncro.inventario.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.syncro.inventario.config.RabbitMQConfig;
import com.syncro.inventario.dto.DescuentoStockRequest;
import com.syncro.inventario.dto.PedidoCreadoEvent;
import com.syncro.inventario.service.InventarioService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PedidoCreadoConsumer {

    private static final Logger log = LoggerFactory.getLogger(PedidoCreadoConsumer.class);
    private final InventarioService inventarioService;

    @RabbitListener(queues = RabbitMQConfig.INVENTARIO_SINCRONIZAR_QUEUE)
    public void handlePedidoCreado(PedidoCreadoEvent event) {
        try {
            log.info("Recibido evento pedido.creado para pedido ID: {}", event.getPedidoId());

            if (event.getItems() != null && !event.getItems().isEmpty()) {
                for (PedidoCreadoEvent.ItemPedido item : event.getItems()) {
                    DescuentoStockRequest request = DescuentoStockRequest.builder()
                            .productoId(item.getProductoId())
                            .pedidoId(event.getPedidoId())
                            .cantidad(item.getCantidad())
                            .build();

                    inventarioService.descontarStock(request);
                    log.info("Stock descontado para producto ID: {}, cantidad: {}", 
                            item.getProductoId(), item.getCantidad());
                }
                log.info("Procesamiento completado para pedido ID: {}", event.getPedidoId());
            } else {
                log.warn("Evento pedido.creado sin items para pedido ID: {}", event.getPedidoId());
            }
        } catch (Exception e) {
            log.error("Error procesando evento pedido.creado para pedido ID: {}", event.getPedidoId(), e);
            throw e;
        }
    }
}

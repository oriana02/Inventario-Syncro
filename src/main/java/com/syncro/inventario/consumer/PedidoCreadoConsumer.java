package com.syncro.inventario.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.syncro.inventario.config.RabbitMQConfig;
import com.syncro.inventario.event.PedidoCreadoEvent;
import com.syncro.inventario.service.InventarioService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Component
@RequiredArgsConstructor
@Getter
@Setter
public class PedidoCreadoConsumer {

    private static final Logger log = LoggerFactory.getLogger(PedidoCreadoConsumer.class);
    private final InventarioService inventarioService;

    @RabbitListener(queues = RabbitMQConfig.COLA_INVENTARIO)
    public void handlePedidoCreado(PedidoCreadoEvent event) {
        log.info("Evento pedido.creado recibido para pedido ID: {}", event.getPedidoId());

        if (event.getItems() == null || event.getItems().isEmpty()) {
            log.warn("El evento pedido.creado para pedido ID: {} no contiene items.", event.getPedidoId());
            return;
        }

        for (PedidoCreadoEvent.ItemEvento item : event.getItems()) {
            try {
                inventarioService.descontarStockPorSku(item.getSku(), event.getEmpresaId(),
                        event.getPedidoId(), item.getCantidad());

                log.info("Stock descontado exitosamente para SKU: {} en pedido ID: {}",
                        item.getSku(), event.getPedidoId());
            } catch (Exception e) {
                log.error("Error al descontar stock para SKU: {} en pedido ID: {}. Error: {}",
                        item.getSku(), event.getPedidoId(), e.getMessage());
            }
        }
    }
}

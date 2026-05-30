package com.syncro.inventario.consumer;

import com.syncro.inventario.event.PedidoCreadoEvent;
import com.syncro.inventario.service.InventarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PedidoCreadoConsumer - Tests Unitarios")
class PedidoCreadoConsumerTest {

    @Mock
    private InventarioService inventarioService;

    @InjectMocks
    private PedidoCreadoConsumer consumer;

    @Test
    @DisplayName("handlePedidoCreado - procesa correctamente un evento con items")
    void handlePedidoCreado_conItems_descuentaStock() {
        PedidoCreadoEvent evento = PedidoCreadoEvent.builder()
                .pedidoId(1L)
                .empresaId(1L)
                .items(List.of(
                        PedidoCreadoEvent.ItemEvento.builder()
                                .sku("ELEC-001").cantidad(2)
                                .precioUnitario(new BigDecimal("29990")).build(),
                        PedidoCreadoEvent.ItemEvento.builder()
                                .sku("ELEC-002").cantidad(1)
                                .precioUnitario(new BigDecimal("4990")).build()
                ))
                .build();

        consumer.handlePedidoCreado(evento);

        verify(inventarioService).descontarStockPorSku("ELEC-001", 1L, 1L, 2);
        verify(inventarioService).descontarStockPorSku("ELEC-002", 1L, 1L, 1);
    }

    @Test
    @DisplayName("handlePedidoCreado - no procesa si la lista de items es nula")
    void handlePedidoCreado_itemsNulos_noDescuenta() {
        PedidoCreadoEvent evento = PedidoCreadoEvent.builder()
                .pedidoId(1L).empresaId(1L).items(null).build();

        consumer.handlePedidoCreado(evento);

        verify(inventarioService, never()).descontarStockPorSku(any(), any(), any(), any());
    }

    @Test
    @DisplayName("handlePedidoCreado - no procesa si la lista de items esta vacia")
    void handlePedidoCreado_itemsVacios_noDescuenta() {
        PedidoCreadoEvent evento = PedidoCreadoEvent.builder()
                .pedidoId(1L).empresaId(1L).items(Collections.emptyList()).build();

        consumer.handlePedidoCreado(evento);

        verify(inventarioService, never()).descontarStockPorSku(any(), any(), any(), any());
    }

    @Test
    @DisplayName("handlePedidoCreado - llama descontarStock una vez por cada item")
    void handlePedidoCreado_tresItems_llamaTresVeces() {
        PedidoCreadoEvent evento = PedidoCreadoEvent.builder()
                .pedidoId(2L).empresaId(1L)
                .items(List.of(
                        PedidoCreadoEvent.ItemEvento.builder().sku("A").cantidad(1).precioUnitario(BigDecimal.ONE).build(),
                        PedidoCreadoEvent.ItemEvento.builder().sku("B").cantidad(2).precioUnitario(BigDecimal.ONE).build(),
                        PedidoCreadoEvent.ItemEvento.builder().sku("C").cantidad(3).precioUnitario(BigDecimal.ONE).build()
                ))
                .build();

        consumer.handlePedidoCreado(evento);

        verify(inventarioService, times(3)).descontarStockPorSku(any(), any(), any(), any());
    }
}

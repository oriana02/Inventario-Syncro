package com.syncro.inventario.event;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PedidoCreadoEvent {

    private Long pedidoId;
    private Long empresaId;
    private List<ItemEvento> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemEvento {

        private String sku;
        private Integer cantidad;
        private BigDecimal precioUnitario;

    }
}

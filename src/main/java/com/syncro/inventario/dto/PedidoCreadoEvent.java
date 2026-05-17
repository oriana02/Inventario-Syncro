package com.syncro.inventario.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PedidoCreadoEvent {
    
    private Long pedidoId;
    private Long empresaId;
    private List<ItemPedido> items;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemPedido {
        private Long productoId;
        private String sku;
        private Integer cantidad;
    }
}

package com.syncro.inventario.event;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true) // Ignorar campos desconocidos en el JSON
public class PedidoCreadoEvent {

    private Long pedidoId;
    private Long empresaId;
    private List<ItemEvento> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true) // Ignorar campos desconocidos en el JSON   
    public static class ItemEvento {

        private String sku;
        private Integer cantidad;
        private BigDecimal precioUnitario;

    }
}

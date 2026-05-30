package com.syncro.inventario.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoResponse {
    
    private Long id;
    private Long empresaId;
    private Long categoriaId;
    private String categoriaNombre;
    private String sku;
    private String nombre;
    private String descripcion;
    private String unidadMedida;
    private Integer stockActual;
    private Integer stockReservado;
    private Integer stockMinimo;
    private BigDecimal precioUnitario;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
}

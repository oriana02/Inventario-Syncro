package com.syncro.inventario.dto;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActualizarProductoRequest {
    private String sku;
    private String nombre;
    private String descripcion;
    private String unidadMedida;
    private Integer stockMinimo;
    private BigDecimal precioUnitario;
    private Long categoriaId;
    private Boolean activo;
}

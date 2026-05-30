package com.syncro.inventario.dto;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrearProductoRequest {
    @NotNull(message = "empresaId es obligatorio")
    private Long empresaId;
    @NotBlank(message = "El SKU es obligatorio")
    private String sku;
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
    private String descripcion;
    private String unidadMedida;
    private Integer stockInicial;
    private Integer stockMinimo;
    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.01")
    private BigDecimal precioUnitario;
    private Long categoriaId;
}

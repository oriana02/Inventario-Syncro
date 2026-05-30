package com.syncro.inventario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AjusteInventarioRequest {
    
    @NotNull(message = "El ID del producto es obligatorio")
    private Long productoId;
    
    @NotNull(message = "El ID de la empresa es obligatorio")
    private Long empresaId;
    
    @NotBlank(message = "El tipo de ajuste es obligatorio")
    @Size(max = 30, message = "El tipo de ajuste no puede exceder 30 caracteres")
    private String tipoAjuste;
    
    @NotNull(message = "La cantidad es obligatoria")
    private Integer cantidad;
    
    @NotBlank(message = "El motivo es obligatorio")
    @Size(max = 300, message = "El motivo no puede exceder 300 caracteres")
    private String motivo;
    
    @NotNull(message = "El ID del usuario es obligatorio")
    private Long usuarioId;
}

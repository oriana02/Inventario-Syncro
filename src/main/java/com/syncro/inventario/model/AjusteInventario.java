package com.syncro.inventario.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ajuste_inventario")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AjusteInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "tipo_ajuste", nullable = false, length = 30)
    private String tipoAjuste;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false, length = 300)
    private String motivo;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movimiento_id")
    private MovimientoInventario movimiento;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime fecha = LocalDateTime.now();
}

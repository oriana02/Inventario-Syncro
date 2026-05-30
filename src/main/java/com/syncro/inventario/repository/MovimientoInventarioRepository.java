package com.syncro.inventario.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.syncro.inventario.model.MovimientoInventario;

@Repository
public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {
    
    List<MovimientoInventario> findByProductoIdOrderByFechaDesc(Long productoId);
    
    List<MovimientoInventario> findByPedidoId(Long pedidoId);
    
    @Query("SELECT m FROM MovimientoInventario m WHERE m.producto.id = :productoId ORDER BY m.fecha DESC")
    List<MovimientoInventario> findLatestMovementsByProducto(@Param("productoId") Long productoId);
}

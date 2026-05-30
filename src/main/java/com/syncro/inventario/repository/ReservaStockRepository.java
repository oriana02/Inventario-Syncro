package com.syncro.inventario.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.syncro.inventario.model.ReservaStock;

@Repository
public interface ReservaStockRepository extends JpaRepository<ReservaStock, Long> {
    
    Optional<ReservaStock> findByProductoIdAndPedidoId(Long productoId, Long pedidoId);
    
    List<ReservaStock> findByPedidoId(Long pedidoId);
    
    List<ReservaStock> findByProductoIdAndEstado(Long productoId, String estado);
    
    @Query("SELECT r FROM ReservaStock r WHERE r.estado = 'ACTIVA' AND r.fechaExpiracion < :now")
    List<ReservaStock> findExpiredReservas(@Param("now") java.time.LocalDateTime now);
    
    @Query("SELECT r FROM ReservaStock r WHERE r.estado = 'ACTIVA'")
    List<ReservaStock> findActiveReservas();
}

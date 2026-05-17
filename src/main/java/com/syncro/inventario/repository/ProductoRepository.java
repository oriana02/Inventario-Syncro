package com.syncro.inventario.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.syncro.inventario.model.Producto;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    
    Optional<Producto> findBySkuAndEmpresaId(String sku, Long empresaId);
    
    List<Producto> findByCategoria_IdAndActivoTrue(Long categoriaId);

    @Query("SELECT p FROM Producto p WHERE p.empresaId = :empresaId AND p.activo = true AND (:categoriaId IS NULL OR p.categoria.id = :categoriaId)")
    List<Producto> findByEmpresaIdAndFilters(@Param("empresaId") Long empresaId, @Param("categoriaId") Long categoriaId);
    
    @Query("SELECT p FROM Producto p WHERE p.stockActual <= p.stockMinimo AND p.activo = true")
    List<Producto> findLowStockProducts();
}

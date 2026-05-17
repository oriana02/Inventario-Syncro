package com.syncro.inventario.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.syncro.inventario.model.AjusteInventario;

@Repository
public interface AjusteInventarioRepository extends JpaRepository<AjusteInventario, Long> {
    
    List<AjusteInventario> findByProductoId(Long productoId);
    
    List<AjusteInventario> findByEmpresaId(Long empresaId);
}

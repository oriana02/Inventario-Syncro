package com.syncro.inventario.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.syncro.inventario.dto.AjusteInventarioRequest;
import com.syncro.inventario.dto.DescuentoStockRequest;
import com.syncro.inventario.dto.ProductoResponse;
import com.syncro.inventario.dto.ReservaStockRequest;
import com.syncro.inventario.model.ReservaStock;
import com.syncro.inventario.service.InventarioService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/inventario")
@RequiredArgsConstructor
public class InventarioController {

    private final InventarioService inventarioService;

    @GetMapping("")
    public String home() {
    return "Microservicio Inventario funcionando";
}

    @GetMapping("/productos")
    public ResponseEntity<List<ProductoResponse>> consultarProductos(
            @RequestParam Long empresaId,
            @RequestParam(required = false) Long categoriaId) {
        List<ProductoResponse> productos = inventarioService.consultarProductos(empresaId, categoriaId);
        return ResponseEntity.ok(productos);
    }

    @GetMapping("/productos/{id}")
    public ResponseEntity<ProductoResponse> consultarProductoPorId(@PathVariable Long id) {
        ProductoResponse producto = inventarioService.consultarProductoPorId(id);
        return ResponseEntity.ok(producto);
    }

    @PatchMapping("/descontar")
    public ResponseEntity<Void> descontarStock(@Valid @RequestBody DescuentoStockRequest request) {
        inventarioService.descontarStock(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/ajuste")
    public ResponseEntity<Void> realizarAjuste(@Valid @RequestBody AjusteInventarioRequest request) {
        inventarioService.realizarAjuste(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/reserva")
    public ResponseEntity<ReservaStock> reservarStock(@Valid @RequestBody ReservaStockRequest request) {
        ReservaStock reserva = inventarioService.reservarStock(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(reserva);
    }

    @PostMapping("/reserva/{id}/liberar")
    public ResponseEntity<Void> liberarReserva(@PathVariable Long id) {
        inventarioService.liberarReserva(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reserva/{id}/confirmar")
    public ResponseEntity<Void> confirmarReserva(@PathVariable Long id) {
        inventarioService.confirmarReserva(id);
        return ResponseEntity.ok().build();
    }
}

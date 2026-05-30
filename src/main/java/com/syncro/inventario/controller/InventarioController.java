package com.syncro.inventario.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.syncro.inventario.dto.AjusteInventarioRequest;
import com.syncro.inventario.dto.DescuentoStockRequest;
import com.syncro.inventario.dto.ProductoResponse;
import com.syncro.inventario.dto.ReservaStockRequest;
import com.syncro.inventario.dto.CrearProductoRequest;
import com.syncro.inventario.dto.ActualizarProductoRequest;
import com.syncro.inventario.model.ReservaStock;
import com.syncro.inventario.service.InventarioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/inventario")
@RequiredArgsConstructor
@Tag(name = "Inventario", description = "API para gestión de inventario")
public class InventarioController {

    private final InventarioService inventarioService;

    @GetMapping("")
    @Operation(summary = "Health check endpoint", description = "Verifica que el microservicio esté funcionando")
    public String home() {
    return "Microservicio Inventario funcionando";
}

    @GetMapping("/productos")
    @Operation(summary = "Consultar productos", description = "Obtiene lista de productos filtrados por empresa y opcionalmente por categoría")
    public ResponseEntity<List<ProductoResponse>> consultarProductos(
            @Parameter(description = "ID de la empresa") @RequestParam Long empresaId,
            @Parameter(description = "ID de la categoría (opcional)") @RequestParam(required = false) Long categoriaId) {
        List<ProductoResponse> productos = inventarioService.consultarProductos(empresaId, categoriaId);
        return ResponseEntity.ok(productos);
    }

    @GetMapping("/productos/{id}")
    @Operation(summary = "Consultar producto por ID", description = "Obtiene un producto específico por su ID")
    public ResponseEntity<ProductoResponse> consultarProductoPorId(
            @Parameter(description = "ID del producto") @PathVariable Long id) {
        ProductoResponse producto = inventarioService.consultarProductoPorId(id);
        return ResponseEntity.ok(producto);
    }

    @PatchMapping("/descontar")
    @Operation(summary = "Descontar stock", description = "Descuenta stock de un producto por venta")
    public ResponseEntity<Void> descontarStock(@Valid @RequestBody DescuentoStockRequest request) {
        inventarioService.descontarStock(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/ajuste")
    @Operation(summary = "Realizar ajuste de inventario", description = "Realiza un ajuste manual de stock (entrada o salida)")
    public ResponseEntity<Void> realizarAjuste(@Valid @RequestBody AjusteInventarioRequest request) {
        inventarioService.realizarAjuste(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/reserva")
    @Operation(summary = "Reservar stock", description = "Reserva stock de un producto para un pedido")
    public ResponseEntity<ReservaStock> reservarStock(@Valid @RequestBody ReservaStockRequest request) {
        ReservaStock reserva = inventarioService.reservarStock(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(reserva);
    }

    @PostMapping("/reserva/{id}/liberar")
    @Operation(summary = "Liberar reserva", description = "Libera una reserva de stock activa")
    public ResponseEntity<Void> liberarReserva(
            @Parameter(description = "ID de la reserva") @PathVariable Long id) {
        inventarioService.liberarReserva(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reserva/{id}/confirmar")
    @Operation(summary = "Confirmar reserva", description = "Confirma una reserva de stock y descuenta el stock definitivamente")
    public ResponseEntity<Void> confirmarReserva(
            @Parameter(description = "ID de la reserva") @PathVariable Long id) {
        inventarioService.confirmarReserva(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/productos")
    @Operation(summary = "Crear producto", description = "Registra un nuevo producto en el inventario")
    public ResponseEntity<ProductoResponse> crearProducto(
            @Valid @RequestBody CrearProductoRequest request) {
        ProductoResponse response = inventarioService.crearProducto(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/productos/{id}")
    @Operation(summary = "Actualizar producto", description = "Modifica los datos de un producto existente")
    public ResponseEntity<ProductoResponse> actualizarProducto(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarProductoRequest request) {
        ProductoResponse response = inventarioService.actualizarProducto(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/productos/{id}")
    @Operation(summary = "Eliminar producto", description = "Desactiva un producto (borrado logico)")
    public ResponseEntity<Void> eliminarProducto(@PathVariable Long id) {
        inventarioService.eliminarProducto(id);
        return ResponseEntity.noContent().build();
    }
}

package com.syncro.inventario.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.syncro.inventario.dto.AjusteInventarioRequest;
import com.syncro.inventario.dto.DescuentoStockRequest;
import com.syncro.inventario.dto.ProductoResponse;
import com.syncro.inventario.dto.ReservaStockRequest;
import com.syncro.inventario.dto.CrearProductoRequest;
import com.syncro.inventario.dto.ActualizarProductoRequest;
import com.syncro.inventario.exception.StockInsuficienteException;
import com.syncro.inventario.exception.ProductoNoEncontradoException;
import com.syncro.inventario.model.AjusteInventario;
import com.syncro.inventario.model.MovimientoInventario;
import com.syncro.inventario.model.Producto;
import com.syncro.inventario.model.ReservaStock;
import com.syncro.inventario.repository.AjusteInventarioRepository;
import com.syncro.inventario.repository.MovimientoInventarioRepository;
import com.syncro.inventario.repository.ProductoRepository;
import com.syncro.inventario.repository.ReservaStockRepository;
import com.syncro.inventario.repository.CategoriaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventarioService {

    private final ProductoRepository productoRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final ReservaStockRepository reservaRepository;
    private final AjusteInventarioRepository ajusteRepository;
    private final CategoriaRepository categoriaRepository;

    @Transactional(readOnly = true)
    public List<ProductoResponse> consultarProductos(Long empresaId, Long categoriaId) {
        List<Producto> productos;
        if (categoriaId != null) {
            productos = productoRepository.findByEmpresaIdAndFilters(empresaId, categoriaId);
        } else {
            productos = productoRepository.findByEmpresaIdAndFilters(empresaId, null);
        }

        return productos.stream()
                .map(this::mapToProductoResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductoResponse consultarProductoPorId(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ProductoNoEncontradoException("Producto no encontrado con ID: " + id));
        return mapToProductoResponse(producto);
    }

    @Transactional
    public void descontarStockPorSku(String sku, Long empresaId, Long pedidoId, Integer cantidad) {
        Producto producto = productoRepository.findBySkuAndEmpresaId(sku, empresaId)
                .orElseThrow(() -> new ProductoNoEncontradoException("Producto no encontrado con SKU: " + sku));

        if (producto.getStockActual() < cantidad) {
            throw new StockInsuficienteException(
                    String.format("Stock insuficiente para producto SKU: %s. Disponible: %d, Solicitado: %d",
                            sku, producto.getStockActual(), cantidad));
        }

        Integer stockAnterior = producto.getStockActual();
        producto.setStockActual(producto.getStockActual() - cantidad);
        productoRepository.save(producto);

        registrarMovimiento(producto, "VENTA", -cantidad,
                stockAnterior, producto.getStockActual(),
                pedidoId, null, "EVENTO_RABBITMQ", "Descuento automatico stock por pedido");

    }

    @Transactional
    public void descontarStock(DescuentoStockRequest request) {
        Producto producto = productoRepository.findById(request.getProductoId())
                .orElseThrow(() -> new ProductoNoEncontradoException("Producto no encontrado con ID: " + request.getProductoId()));

        if (producto.getStockActual() < request.getCantidad()) {
            throw new StockInsuficienteException(
                    String.format("Stock insuficiente para producto ID: %d. Disponible: %d, Solicitado: %d",
                            request.getProductoId(), producto.getStockActual(), request.getCantidad()));
        }

        Integer stockAnterior = producto.getStockActual();
        producto.setStockActual(producto.getStockActual() - request.getCantidad());
        productoRepository.save(producto);

        registrarMovimiento(producto, "VENTA", -request.getCantidad(),
                stockAnterior, producto.getStockActual(),
                request.getPedidoId(), null, "USUARIO", "Descuento de stock por venta");
    }

    @Transactional
    public void realizarAjuste(AjusteInventarioRequest request) {
        Producto producto = productoRepository.findById(request.getProductoId())
                .orElseThrow(() -> new ProductoNoEncontradoException("Producto no encontrado con ID: " + request.getProductoId()));

        Integer stockAnterior = producto.getStockActual();
        Integer stockNuevo = stockAnterior + request.getCantidad();

        if (stockNuevo < 0) {
            throw new StockInsuficienteException("El ajuste resultaría en stock negativo");
        }

        producto.setStockActual(stockNuevo);
        productoRepository.save(producto);

        String tipoMovimiento = request.getCantidad() > 0 ? "AJUSTE_ENTRADA" : "AJUSTE_SALIDA";
        MovimientoInventario movimiento = registrarMovimiento(producto, tipoMovimiento, request.getCantidad(),
                stockAnterior, stockNuevo, null, request.getUsuarioId(), "USUARIO", request.getMotivo());

        AjusteInventario ajuste = AjusteInventario.builder()
                .producto(producto)
                .empresaId(request.getEmpresaId())
                .tipoAjuste(request.getTipoAjuste())
                .cantidad(request.getCantidad())
                .motivo(request.getMotivo())
                .usuarioId(request.getUsuarioId())
                .movimiento(movimiento)
                .fecha(LocalDateTime.now())
                .build();
        ajusteRepository.save(ajuste);
    }

    @Transactional
    public ReservaStock reservarStock(ReservaStockRequest request) {
        Producto producto = productoRepository.findById(request.getProductoId())
                .orElseThrow(() -> new ProductoNoEncontradoException("Producto no encontrado con ID: " + request.getProductoId()));

        Integer stockDisponible = producto.getStockActual() - producto.getStockReservado();
        if (stockDisponible < request.getCantidad()) {
            throw new StockInsuficienteException(
                    String.format("Stock insuficiente para reserva. Disponible: %d, Solicitado: %d",
                            stockDisponible, request.getCantidad()));
        }

        producto.setStockReservado(producto.getStockReservado() + request.getCantidad());
        productoRepository.save(producto);

        Integer minutosExpiracion = request.getMinutosExpiracion() != null ? request.getMinutosExpiracion() : 30;
        LocalDateTime fechaExpiracion = LocalDateTime.now().plusMinutes(minutosExpiracion);

        ReservaStock reserva = ReservaStock.builder()
                .producto(producto)
                .pedidoId(request.getPedidoId())
                .cantidad(request.getCantidad())
                .estado("ACTIVA")
                .fechaCreacion(LocalDateTime.now())
                .fechaExpiracion(fechaExpiracion)
                .build();

        ReservaStock savedReserva = reservaRepository.save(reserva);

        registrarMovimiento(producto, "RESERVA", request.getCantidad(),
                producto.getStockActual(), producto.getStockActual(),
                request.getPedidoId(), null, "SISTEMA", "Reserva de stock");

        return savedReserva;
    }

    @Transactional
    public void liberarReserva(Long reservaId) {
        ReservaStock reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ProductoNoEncontradoException("Reserva no encontrada con ID: " + reservaId));

        if (!"ACTIVA".equals(reserva.getEstado())) {
            throw new IllegalStateException("La reserva no está en estado ACTIVA");
        }

        Producto producto = reserva.getProducto();
        producto.setStockReservado(producto.getStockReservado() - reserva.getCantidad());
        productoRepository.save(producto);

        reserva.setEstado("LIBERADA");
        reserva.setFechaResolucion(LocalDateTime.now());
        reservaRepository.save(reserva);

        registrarMovimiento(producto, "LIBERACION_RESERVA", reserva.getCantidad(),
                producto.getStockActual(), producto.getStockActual(),
                reserva.getPedidoId(), null, "SISTEMA", "Liberación de reserva");
    }

    @Transactional
    public void confirmarReserva(Long reservaId) {
        ReservaStock reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ProductoNoEncontradoException("Reserva no encontrada con ID: " + reservaId));

        if (!"ACTIVA".equals(reserva.getEstado())) {
            throw new IllegalStateException("La reserva no está en estado ACTIVA");
        }

        Producto producto = reserva.getProducto();
        Integer stockAnterior = producto.getStockActual();
        producto.setStockActual(producto.getStockActual() - reserva.getCantidad());
        producto.setStockReservado(producto.getStockReservado() - reserva.getCantidad());
        productoRepository.save(producto);

        reserva.setEstado("CONFIRMADA");
        reserva.setFechaResolucion(LocalDateTime.now());
        reservaRepository.save(reserva);

        registrarMovimiento(producto, "VENTA", -reserva.getCantidad(),
                stockAnterior, producto.getStockActual(),
                reserva.getPedidoId(), null, "SISTEMA", "Confirmación de reserva");
    }

    @Transactional
    public void liberarReservasExpiradas() {
        List<ReservaStock> reservasExpiradas = reservaRepository.findExpiredReservas(LocalDateTime.now());

        for (ReservaStock reserva : reservasExpiradas) {
            try {
                liberarReserva(reserva.getId());
                reserva.setEstado("EXPIRADA");
                reservaRepository.save(reserva);
            } catch (Exception e) {
                // Log error but continue with other reservations
                System.err.println("Error al liberar reserva expirada ID: " + reserva.getId() + ", Error: " + e.getMessage());
            }
        }
    }

    private MovimientoInventario registrarMovimiento(Producto producto, String tipo, Integer cantidad,
            Integer stockAnterior, Integer stockPosterior, Long pedidoId, Long usuarioId,
            String origen, String motivo) {
        MovimientoInventario movimiento = MovimientoInventario.builder()
                .producto(producto)
                .tipo(tipo)
                .cantidad(cantidad)
                .stockAnterior(stockAnterior)
                .stockPosterior(stockPosterior)
                .pedidoId(pedidoId)
                .usuarioId(usuarioId)
                .origen(origen)
                .motivo(motivo)
                .fecha(LocalDateTime.now())
                .build();
        return movimientoRepository.save(movimiento);
    }

    public ProductoResponse crearProducto(CrearProductoRequest request) {
        Producto producto = Producto.builder()
                .empresaId(request.getEmpresaId())
                .sku(request.getSku())
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .unidadMedida(request.getUnidadMedida() != null ? request.getUnidadMedida() : "UNIDAD")
                .stockActual(request.getStockInicial() != null ? request.getStockInicial() : 0)
                .stockMinimo(request.getStockMinimo() != null ? request.getStockMinimo() : 0)
                .precioUnitario(request.getPrecioUnitario())
                .activo(true)
                .build();
        if (request.getCategoriaId() != null) {
            categoriaRepository.findById(request.getCategoriaId())
                .ifPresent(producto::setCategoria);
        }
        return mapToProductoResponse(productoRepository.save(producto));
    }

    public ProductoResponse actualizarProducto(Long id, ActualizarProductoRequest request) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ProductoNoEncontradoException("Producto no encontrado: " + id));
        if (request.getNombre() != null) producto.setNombre(request.getNombre());
        if (request.getDescripcion() != null) producto.setDescripcion(request.getDescripcion());
        if (request.getPrecioUnitario() != null) producto.setPrecioUnitario(request.getPrecioUnitario());
        if (request.getStockMinimo() != null) producto.setStockMinimo(request.getStockMinimo());
        if (request.getActivo() != null) producto.setActivo(request.getActivo());
        if (request.getCategoriaId() != null) {
            categoriaRepository.findById(request.getCategoriaId())
                .ifPresent(producto::setCategoria);
        }
        return mapToProductoResponse(productoRepository.save(producto));
    }

    public void eliminarProducto(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ProductoNoEncontradoException("Producto no encontrado: " + id));
        producto.setActivo(false);
        productoRepository.save(producto);
    }

    private ProductoResponse mapToProductoResponse(Producto producto) {
        return ProductoResponse.builder()
                .id(producto.getId())
                .empresaId(producto.getEmpresaId())
                .categoriaId(producto.getCategoria() != null ? producto.getCategoria().getId() : null)
                .categoriaNombre(producto.getCategoria() != null ? producto.getCategoria().getNombre() : null)
                .sku(producto.getSku())
                .nombre(producto.getNombre())
                .descripcion(producto.getDescripcion())
                .unidadMedida(producto.getUnidadMedida())
                .stockActual(producto.getStockActual())
                .stockReservado(producto.getStockReservado())
                .stockMinimo(producto.getStockMinimo())
                .precioUnitario(producto.getPrecioUnitario())
                .activo(producto.getActivo())
                .fechaCreacion(producto.getFechaCreacion())
                .fechaActualizacion(producto.getFechaActualizacion())
                .build();
    }
}

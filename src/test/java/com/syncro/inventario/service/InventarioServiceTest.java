package com.syncro.inventario.service;

import com.syncro.inventario.dto.AjusteInventarioRequest;
import com.syncro.inventario.dto.CrearProductoRequest;
import com.syncro.inventario.dto.DescuentoStockRequest;
import com.syncro.inventario.dto.ProductoResponse;
import com.syncro.inventario.dto.ReservaStockRequest;
import com.syncro.inventario.exception.ProductoNoEncontradoException;
import com.syncro.inventario.exception.StockInsuficienteException;
import com.syncro.inventario.model.Categoria;
import com.syncro.inventario.model.MovimientoInventario;
import com.syncro.inventario.model.Producto;
import com.syncro.inventario.model.ReservaStock;
import com.syncro.inventario.repository.AjusteInventarioRepository;
import com.syncro.inventario.repository.CategoriaRepository;
import com.syncro.inventario.repository.MovimientoInventarioRepository;
import com.syncro.inventario.repository.ProductoRepository;
import com.syncro.inventario.repository.ReservaStockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventarioService - Tests Unitarios")
class InventarioServiceTest {

    @Mock private ProductoRepository productoRepository;
    @Mock private MovimientoInventarioRepository movimientoRepository;
    @Mock private ReservaStockRepository reservaRepository;
    @Mock private AjusteInventarioRepository ajusteRepository;
    @Mock private CategoriaRepository categoriaRepository;

    @InjectMocks
    private InventarioService inventarioService;

    private Producto productoMock;
    private Categoria categoriaMock;

    @BeforeEach
    void setUp() {
        categoriaMock = new Categoria(1L, "Electrónica", "Dispositivos", true);

        productoMock = Producto.builder()
                .id(1L)
                .empresaId(1L)
                .sku("ELEC-001")
                .nombre("Audífonos Bluetooth")
                .descripcion("Audífonos inalámbricos")
                .stockActual(50)
                .stockReservado(0)
                .stockMinimo(10)
                .precioUnitario(new BigDecimal("29990"))
                .categoria(categoriaMock)
                .activo(true)
                .build();
    }

    // =========================================================================
    // consultarProductos
    // =========================================================================

    @Test
    @DisplayName("consultarProductos - retorna lista de productos de la empresa")
    void consultarProductos_retornaLista() {
        when(productoRepository.findByEmpresaIdAndFilters(1L, null))
                .thenReturn(List.of(productoMock));

        List<ProductoResponse> resultado = inventarioService.consultarProductos(1L, null);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getSku()).isEqualTo("ELEC-001");
        assertThat(resultado.get(0).getStockActual()).isEqualTo(50);
    }

    @Test
    @DisplayName("consultarProductos - lista vacía si no hay productos")
    void consultarProductos_listaVacia() {
        when(productoRepository.findByEmpresaIdAndFilters(99L, null))
                .thenReturn(List.of());

        List<ProductoResponse> resultado = inventarioService.consultarProductos(99L, null);

        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("consultarProductos - filtra por categoría cuando se proporciona")
    void consultarProductos_conCategoria() {
        when(productoRepository.findByEmpresaIdAndFilters(1L, 1L))
                .thenReturn(List.of(productoMock));

        List<ProductoResponse> resultado = inventarioService.consultarProductos(1L, 1L);

        assertThat(resultado).hasSize(1);
        verify(productoRepository).findByEmpresaIdAndFilters(1L, 1L);
    }

    // =========================================================================
    // consultarProductoPorId
    // =========================================================================

    @Test
    @DisplayName("consultarProductoPorId - retorna producto existente")
    void consultarProductoPorId_productoExistente() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(productoMock));

        ProductoResponse resultado = inventarioService.consultarProductoPorId(1L);

        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("Audífonos Bluetooth");
        assertThat(resultado.getCategoriaNombre()).isEqualTo("Electrónica");
    }

    @Test
    @DisplayName("consultarProductoPorId - lanza excepcion si no existe")
    void consultarProductoPorId_noExiste_lanzaExcepcion() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventarioService.consultarProductoPorId(99L))
                .isInstanceOf(ProductoNoEncontradoException.class)
                .hasMessageContaining("99");
    }

    // =========================================================================
    // descontarStockPorSku
    // =========================================================================

    @Test
    @DisplayName("descontarStockPorSku - descuenta correctamente el stock")
    void descontarStockPorSku_stockSuficiente_descuenta() {
        when(productoRepository.findBySkuAndEmpresaId("ELEC-001", 1L))
                .thenReturn(Optional.of(productoMock));
        when(productoRepository.save(any())).thenReturn(productoMock);
        when(movimientoRepository.save(any())).thenReturn(new MovimientoInventario());

        inventarioService.descontarStockPorSku("ELEC-001", 1L, 10L, 5);

        assertThat(productoMock.getStockActual()).isEqualTo(45);
        verify(productoRepository).save(productoMock);
        verify(movimientoRepository).save(any());
    }

    @Test
    @DisplayName("descontarStockPorSku - lanza excepcion si stock insuficiente")
    void descontarStockPorSku_stockInsuficiente_lanzaExcepcion() {
        when(productoRepository.findBySkuAndEmpresaId("ELEC-001", 1L))
                .thenReturn(Optional.of(productoMock));

        assertThatThrownBy(() ->
                inventarioService.descontarStockPorSku("ELEC-001", 1L, 10L, 100))
                .isInstanceOf(StockInsuficienteException.class)
                .hasMessageContaining("ELEC-001");

        verify(productoRepository, never()).save(any());
    }

    @Test
    @DisplayName("descontarStockPorSku - lanza excepcion si SKU no existe")
    void descontarStockPorSku_skuNoExiste_lanzaExcepcion() {
        when(productoRepository.findBySkuAndEmpresaId("SKU-999", 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                inventarioService.descontarStockPorSku("SKU-999", 1L, 10L, 5))
                .isInstanceOf(ProductoNoEncontradoException.class)
                .hasMessageContaining("SKU-999");
    }

    // =========================================================================
    // descontarStock
    // =========================================================================

    @Test
    @DisplayName("descontarStock - descuenta por ID de producto correctamente")
    void descontarStock_stockSuficiente_descuenta() {
        DescuentoStockRequest request = DescuentoStockRequest.builder()
                .productoId(1L).pedidoId(5L).cantidad(10).build();

        when(productoRepository.findById(1L)).thenReturn(Optional.of(productoMock));
        when(productoRepository.save(any())).thenReturn(productoMock);
        when(movimientoRepository.save(any())).thenReturn(new MovimientoInventario());

        inventarioService.descontarStock(request);

        assertThat(productoMock.getStockActual()).isEqualTo(40);
        verify(movimientoRepository).save(any());
    }

    @Test
    @DisplayName("descontarStock - lanza excepcion si stock insuficiente")
    void descontarStock_stockInsuficiente_lanzaExcepcion() {
        DescuentoStockRequest request = DescuentoStockRequest.builder()
                .productoId(1L).pedidoId(5L).cantidad(200).build();

        when(productoRepository.findById(1L)).thenReturn(Optional.of(productoMock));

        assertThatThrownBy(() -> inventarioService.descontarStock(request))
                .isInstanceOf(StockInsuficienteException.class);

        verify(productoRepository, never()).save(any());
    }

    // =========================================================================
    // realizarAjuste
    // =========================================================================

    @Test
    @DisplayName("realizarAjuste - ajuste de entrada aumenta el stock")
    void realizarAjuste_entrada_aumentaStock() {
        AjusteInventarioRequest request = AjusteInventarioRequest.builder()
                .productoId(1L).empresaId(1L).tipoAjuste("INGRESO_MERCADERIA")
                .cantidad(20).motivo("Ingreso nuevo lote").usuarioId(1L).build();

        when(productoRepository.findById(1L)).thenReturn(Optional.of(productoMock));
        when(productoRepository.save(any())).thenReturn(productoMock);
        when(movimientoRepository.save(any())).thenReturn(new MovimientoInventario());

        inventarioService.realizarAjuste(request);

        assertThat(productoMock.getStockActual()).isEqualTo(70);
        verify(ajusteRepository).save(any());
    }

    @Test
    @DisplayName("realizarAjuste - ajuste negativo que resulta en stock negativo lanza excepcion")
    void realizarAjuste_resultadoNegativo_lanzaExcepcion() {
        AjusteInventarioRequest request = AjusteInventarioRequest.builder()
                .productoId(1L).empresaId(1L).tipoAjuste("MERMA")
                .cantidad(-100).motivo("Merma").usuarioId(1L).build();

        when(productoRepository.findById(1L)).thenReturn(Optional.of(productoMock));

        assertThatThrownBy(() -> inventarioService.realizarAjuste(request))
                .isInstanceOf(StockInsuficienteException.class)
                .hasMessageContaining("negativo");

        verify(productoRepository, never()).save(any());
    }

    // =========================================================================
    // reservarStock
    // =========================================================================

    @Test
    @DisplayName("reservarStock - reserva exitosa aumenta stockReservado")
    void reservarStock_stockDisponible_reservaExitosa() {
        ReservaStockRequest request = ReservaStockRequest.builder()
                .productoId(1L).pedidoId(5L).cantidad(10).minutosExpiracion(30).build();

        when(productoRepository.findById(1L)).thenReturn(Optional.of(productoMock));
        when(productoRepository.save(any())).thenReturn(productoMock);
        when(reservaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(movimientoRepository.save(any())).thenReturn(new MovimientoInventario());

        ReservaStock reserva = inventarioService.reservarStock(request);

        assertThat(productoMock.getStockReservado()).isEqualTo(10);
        assertThat(reserva.getEstado()).isEqualTo("ACTIVA");
        assertThat(reserva.getCantidad()).isEqualTo(10);
    }

    @Test
    @DisplayName("reservarStock - lanza excepcion si no hay stock disponible")
    void reservarStock_sinStockDisponible_lanzaExcepcion() {
        productoMock.setStockReservado(45);
        ReservaStockRequest request = ReservaStockRequest.builder()
                .productoId(1L).pedidoId(5L).cantidad(10).build();

        when(productoRepository.findById(1L)).thenReturn(Optional.of(productoMock));

        assertThatThrownBy(() -> inventarioService.reservarStock(request))
                .isInstanceOf(StockInsuficienteException.class);
    }

    // =========================================================================
    // liberarReserva
    // =========================================================================

    @Test
    @DisplayName("liberarReserva - libera correctamente una reserva activa")
    void liberarReserva_reservaActiva_liberaExitoso() {
        ReservaStock reserva = ReservaStock.builder()
                .id(1L).producto(productoMock).pedidoId(5L)
                .cantidad(10).estado("ACTIVA")
                .fechaExpiracion(LocalDateTime.now().plusMinutes(30)).build();

        productoMock.setStockReservado(10);
        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));
        when(productoRepository.save(any())).thenReturn(productoMock);
        when(reservaRepository.save(any())).thenReturn(reserva);
        when(movimientoRepository.save(any())).thenReturn(new MovimientoInventario());

        inventarioService.liberarReserva(1L);

        assertThat(reserva.getEstado()).isEqualTo("LIBERADA");
        assertThat(productoMock.getStockReservado()).isEqualTo(0);
    }

    @Test
    @DisplayName("liberarReserva - lanza excepcion si reserva no está activa")
    void liberarReserva_reservaNoActiva_lanzaExcepcion() {
        ReservaStock reserva = ReservaStock.builder()
                .id(1L).producto(productoMock).pedidoId(5L)
                .cantidad(10).estado("LIBERADA")
                .fechaExpiracion(LocalDateTime.now().plusMinutes(30)).build();

        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));

        assertThatThrownBy(() -> inventarioService.liberarReserva(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ACTIVA");
    }

    // =========================================================================
    // crearProducto
    // =========================================================================

    @Test
    @DisplayName("crearProducto - crea producto con datos validos")
    void crearProducto_datosValidos_creaExitoso() {
        CrearProductoRequest request = CrearProductoRequest.builder()
                .empresaId(1L).sku("NUEVO-001").nombre("Nuevo Producto")
                .precioUnitario(new BigDecimal("9990"))
                .stockInicial(100).stockMinimo(5).build();

        when(productoRepository.save(any())).thenReturn(productoMock);

        ProductoResponse response = inventarioService.crearProducto(request);

        assertThat(response).isNotNull();
        verify(productoRepository).save(any(Producto.class));
    }

    // =========================================================================
    // eliminarProducto
    // =========================================================================

    @Test
    @DisplayName("eliminarProducto - desactiva el producto (borrado logico)")
    void eliminarProducto_productoExistente_desactiva() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(productoMock));
        when(productoRepository.save(any())).thenReturn(productoMock);

        inventarioService.eliminarProducto(1L);

        assertThat(productoMock.getActivo()).isFalse();
        verify(productoRepository).save(productoMock);
    }

    @Test
    @DisplayName("eliminarProducto - lanza excepcion si producto no existe")
    void eliminarProducto_noExiste_lanzaExcepcion() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventarioService.eliminarProducto(99L))
                .isInstanceOf(ProductoNoEncontradoException.class);
    }
}

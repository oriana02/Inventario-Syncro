package com.syncro.inventario.service;

import com.syncro.inventario.dto.*;
import com.syncro.inventario.exception.ProductoNoEncontradoException;
import com.syncro.inventario.exception.StockInsuficienteException;
import com.syncro.inventario.model.*;
import com.syncro.inventario.repository.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventarioServiceTest {

    @Mock
    private ProductoRepository productoRepository;
    @Mock
    private MovimientoInventarioRepository movimientoRepository;
    @Mock
    private ReservaStockRepository reservaRepository;
    @Mock
    private AjusteInventarioRepository ajusteRepository;
    @Mock
    private CategoriaRepository categoriaRepository;
    @Mock
    private ApplicationContext applicationContext;

    @InjectMocks
    private InventarioService service;

    private Producto productoBase;

    @BeforeEach
    void setUp() {
        productoBase = Producto.builder()
                .id(1L)
                .empresaId(10L)
                .sku("SKU-001")
                .nombre("Producto Test")
                .stockActual(100)
                .stockReservado(0)
                .stockMinimo(5)
                .precioUnitario(BigDecimal.valueOf(9.99))
                .activo(true)
                .build();

        ReflectionTestUtils.setField(service, "applicationContext", applicationContext);
    }

    // ── consultarProductos ────────────────────────────────────────────────────
    @Test
    void consultarProductos_conCategoria_filtraCorrectamente() {
        when(productoRepository.findByEmpresaIdAndFilters(10L, 2L))
                .thenReturn(List.of(productoBase));

        List<ProductoResponse> result = service.consultarProductos(10L, 2L);

        assertEquals(1, result.size());
    }

    @Test
    void consultarProductos_sinCategoria_retornaTodos() {
        when(productoRepository.findByEmpresaIdAndFilters(10L, null))
                .thenReturn(List.of(productoBase));

        assertEquals(1, service.consultarProductos(10L, null).size());
    }

    // ── consultarProductoPorId ────────────────────────────────────────────────
    @Test
    void consultarProductoPorId_existente_retornaResponse() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(productoBase));

        assertEquals(1L, service.consultarProductoPorId(1L).getId());
    }

    @Test
    void consultarProductoPorId_noExistente_lanzaExcepcion() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ProductoNoEncontradoException.class,
                () -> service.consultarProductoPorId(99L));
    }

    @Test
    void consultarProductoPorId_conCategoria_mapeaCategoria() {
        Categoria categoria = new Categoria();
        categoria.setId(2L);
        categoria.setNombre("Bebidas");
        productoBase.setCategoria(categoria);

        when(productoRepository.findById(1L)).thenReturn(Optional.of(productoBase));

        ProductoResponse response = service.consultarProductoPorId(1L);
        assertEquals(2L, response.getCategoriaId());
        assertEquals("Bebidas", response.getCategoriaNombre());
    }

    @Test
    void consultarProductoPorId_sinCategoria_categoriaIdEsNull() {
        productoBase.setCategoria(null);
        when(productoRepository.findById(1L)).thenReturn(Optional.of(productoBase));

        ProductoResponse response = service.consultarProductoPorId(1L);
        assertNull(response.getCategoriaId());
        assertNull(response.getCategoriaNombre());
    }

    // ── descontarStockPorSku ──────────────────────────────────────────────────
    @Test
    void descontarStockPorSku_exitoso_reducStock() {
        when(productoRepository.findBySkuAndEmpresaId("SKU-001", 10L))
                .thenReturn(Optional.of(productoBase));
        when(productoRepository.save(any())).thenReturn(productoBase);
        when(movimientoRepository.save(any())).thenReturn(new MovimientoInventario());

        service.descontarStockPorSku("SKU-001", 10L, 1L, 10);

        assertEquals(90, productoBase.getStockActual());
    }

    @Test
    void descontarStockPorSku_skuNoExiste_lanzaExcepcion() {
        when(productoRepository.findBySkuAndEmpresaId("NOEXISTE", 10L))
                .thenReturn(Optional.empty());

        assertThrows(ProductoNoEncontradoException.class,
                () -> service.descontarStockPorSku("NOEXISTE", 10L, 1L, 5));
    }

    @Test
    void descontarStockPorSku_stockInsuficiente_lanzaExcepcion() {
        when(productoRepository.findBySkuAndEmpresaId("SKU-001", 10L))
                .thenReturn(Optional.of(productoBase));

        assertThrows(StockInsuficienteException.class,
                () -> service.descontarStockPorSku("SKU-001", 10L, 1L, 200));
    }

    // ── descontarStock ────────────────────────────────────────────────────────
    @Test
    void descontarStock_exitoso_reducStock() {
        DescuentoStockRequest request = new DescuentoStockRequest();
        request.setProductoId(1L);
        request.setCantidad(20);
        request.setPedidoId(5L);

        when(productoRepository.findById(1L)).thenReturn(Optional.of(productoBase));
        when(productoRepository.save(any())).thenReturn(productoBase);
        when(movimientoRepository.save(any())).thenReturn(new MovimientoInventario());

        service.descontarStock(request);

        assertEquals(80, productoBase.getStockActual());
    }

    @Test
    void descontarStock_productoNoEncontrado_lanzaExcepcion() {
        DescuentoStockRequest request = new DescuentoStockRequest();
        request.setProductoId(99L);
        request.setCantidad(5);

        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ProductoNoEncontradoException.class, () -> service.descontarStock(request));
    }

    @Test
    void descontarStock_stockInsuficiente_lanzaExcepcion() {
        DescuentoStockRequest request = new DescuentoStockRequest();
        request.setProductoId(1L);
        request.setCantidad(999);

        when(productoRepository.findById(1L)).thenReturn(Optional.of(productoBase));

        assertThrows(StockInsuficienteException.class, () -> service.descontarStock(request));
    }

    // ── realizarAjuste ────────────────────────────────────────────────────────
    @Test
    void realizarAjuste_positivo_incrementaStock() {
        AjusteInventarioRequest request = new AjusteInventarioRequest();
        request.setProductoId(1L);
        request.setCantidad(50);
        request.setEmpresaId(10L);
        request.setTipoAjuste("ENTRADA");
        request.setMotivo("Reposicion");
        request.setUsuarioId(1L);

        when(productoRepository.findById(1L)).thenReturn(Optional.of(productoBase));
        when(productoRepository.save(any())).thenReturn(productoBase);
        when(movimientoRepository.save(any())).thenReturn(MovimientoInventario.builder().id(1L).build());
        when(ajusteRepository.save(any())).thenReturn(new AjusteInventario());

        service.realizarAjuste(request);

        assertEquals(150, productoBase.getStockActual());
    }

    @Test
    void realizarAjuste_negativo_decrementaStock() {
        AjusteInventarioRequest request = new AjusteInventarioRequest();
        request.setProductoId(1L);
        request.setCantidad(-30);
        request.setEmpresaId(10L);
        request.setTipoAjuste("SALIDA");
        request.setMotivo("Merma");
        request.setUsuarioId(1L);

        when(productoRepository.findById(1L)).thenReturn(Optional.of(productoBase));
        when(productoRepository.save(any())).thenReturn(productoBase);
        when(movimientoRepository.save(any())).thenReturn(MovimientoInventario.builder().id(1L).build());
        when(ajusteRepository.save(any())).thenReturn(new AjusteInventario());

        service.realizarAjuste(request);

        assertEquals(70, productoBase.getStockActual());
    }

    @Test
    void realizarAjuste_resultadoNegativo_lanzaExcepcion() {
        AjusteInventarioRequest request = new AjusteInventarioRequest();
        request.setProductoId(1L);
        request.setCantidad(-999);

        when(productoRepository.findById(1L)).thenReturn(Optional.of(productoBase));

        assertThrows(StockInsuficienteException.class, () -> service.realizarAjuste(request));
    }

    // ── reservarStock ─────────────────────────────────────────────────────────
    @Test
    void reservarStock_exitoso_incrementaStockReservado() {
        ReservaStockRequest request = new ReservaStockRequest();
        request.setProductoId(1L);
        request.setCantidad(10);
        request.setPedidoId(7L);
        request.setMinutosExpiracion(60);

        when(productoRepository.findById(1L)).thenReturn(Optional.of(productoBase));
        when(productoRepository.save(any())).thenReturn(productoBase);
        when(reservaRepository.save(any())).thenReturn(ReservaStock.builder().id(1L).build());
        when(movimientoRepository.save(any())).thenReturn(new MovimientoInventario());

        assertNotNull(service.reservarStock(request));
        assertEquals(10, productoBase.getStockReservado());
    }

    @Test
    void reservarStock_sinMinutosExpiracion_usaDefault30() {
        ReservaStockRequest request = new ReservaStockRequest();
        request.setProductoId(1L);
        request.setCantidad(5);
        request.setPedidoId(8L);
        request.setMinutosExpiracion(null); // rama null → default 30

        when(productoRepository.findById(1L)).thenReturn(Optional.of(productoBase));
        when(productoRepository.save(any())).thenReturn(productoBase);
        when(reservaRepository.save(any())).thenReturn(ReservaStock.builder().id(2L).build());
        when(movimientoRepository.save(any())).thenReturn(new MovimientoInventario());

        assertNotNull(service.reservarStock(request));
    }

    @Test
    void reservarStock_stockInsuficiente_lanzaExcepcion() {
        ReservaStockRequest request = new ReservaStockRequest();
        request.setProductoId(1L);
        request.setCantidad(500);

        when(productoRepository.findById(1L)).thenReturn(Optional.of(productoBase));

        assertThrows(StockInsuficienteException.class, () -> service.reservarStock(request));
    }

    // ── liberarReserva ────────────────────────────────────────────────────────
    @Test
    void liberarReserva_exitoso_cambiaEstadoALiberada() {
        productoBase.setStockReservado(10);
        ReservaStock reserva = ReservaStock.builder()
                .id(1L).producto(productoBase).cantidad(10).estado("ACTIVA").pedidoId(1L).build();

        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));
        when(productoRepository.save(any())).thenReturn(productoBase);
        when(reservaRepository.save(any())).thenReturn(reserva);
        when(movimientoRepository.save(any())).thenReturn(new MovimientoInventario());

        service.liberarReserva(1L);

        assertEquals("LIBERADA", reserva.getEstado());
        assertEquals(0, productoBase.getStockReservado());
    }

    @Test
    void liberarReserva_noActiva_lanzaIllegalState() {
        ReservaStock reserva = ReservaStock.builder()
                .id(1L).producto(productoBase).estado("CONFIRMADA").build();

        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));

        assertThrows(IllegalStateException.class, () -> service.liberarReserva(1L));
    }

    @Test
    void liberarReserva_noExiste_lanzaExcepcion() {
        when(reservaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ProductoNoEncontradoException.class, () -> service.liberarReserva(99L));
    }

    // ── confirmarReserva ──────────────────────────────────────────────────────
    @Test
    void confirmarReserva_exitoso_cambiaEstadoYReduceStock() {
        productoBase.setStockReservado(10);
        ReservaStock reserva = ReservaStock.builder()
                .id(1L).producto(productoBase).cantidad(10).estado("ACTIVA").pedidoId(2L).build();

        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));
        when(productoRepository.save(any())).thenReturn(productoBase);
        when(reservaRepository.save(any())).thenReturn(reserva);
        when(movimientoRepository.save(any())).thenReturn(new MovimientoInventario());

        service.confirmarReserva(1L);

        assertEquals("CONFIRMADA", reserva.getEstado());
        assertEquals(90, productoBase.getStockActual());
        assertEquals(0, productoBase.getStockReservado());
    }

    @Test
    void confirmarReserva_noActiva_lanzaIllegalState() {
        ReservaStock reserva = ReservaStock.builder()
                .id(1L).producto(productoBase).estado("LIBERADA").build();

        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));

        assertThrows(IllegalStateException.class, () -> service.confirmarReserva(1L));
    }

    // ── liberarReservasExpiradas ──────────────────────────────────────────────
    @Test
    void liberarReservasExpiradas_sinReservas_noHaceNada() {
        when(reservaRepository.findExpiredReservas(any())).thenReturn(List.of());

        assertDoesNotThrow(() -> service.liberarReservasExpiradas());
    }

    @Test
    void liberarReservasExpiradas_conReservaActiva_liberaYMarcaExpirada() {
        productoBase.setStockReservado(5);
        ReservaStock reserva = ReservaStock.builder()
                .id(1L).producto(productoBase).cantidad(5).estado("ACTIVA").pedidoId(3L).build();

        when(reservaRepository.findExpiredReservas(any())).thenReturn(List.of(reserva));
        when(applicationContext.getBean(InventarioService.class)).thenReturn(service);
        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));
        when(productoRepository.save(any())).thenReturn(productoBase);
        when(reservaRepository.save(any())).thenReturn(reserva);
        when(movimientoRepository.save(any())).thenReturn(new MovimientoInventario());

        service.liberarReservasExpiradas();

        assertEquals("EXPIRADA", reserva.getEstado());
    }

    @Test
    void liberarReservasExpiradas_errorEnReserva_continuaSinPropagar() {
        ReservaStock reserva = ReservaStock.builder()
                .id(2L).producto(productoBase).cantidad(5).estado("ACTIVA").build();

        when(reservaRepository.findExpiredReservas(any())).thenReturn(List.of(reserva));
        when(applicationContext.getBean(InventarioService.class)).thenReturn(service);
        when(reservaRepository.findById(2L))
                .thenThrow(new RuntimeException("Error de base de datos"));

        assertDoesNotThrow(() -> service.liberarReservasExpiradas());
    }

    // ── crearProducto ─────────────────────────────────────────────────────────
    @Test
    void crearProducto_sinCategoriaId_creaProductoExitosamente() {
        CrearProductoRequest request = new CrearProductoRequest();
        request.setEmpresaId(10L);
        request.setSku("NEW-001");
        request.setNombre("Nuevo Producto");
        request.setStockInicial(50);
        request.setStockMinimo(5);
        request.setPrecioUnitario(BigDecimal.valueOf(15.00));

        when(productoRepository.save(any())).thenReturn(productoBase);

        assertNotNull(service.crearProducto(request));
    }

    @Test
    void crearProducto_conCategoriaId_asignaCategoria() {
        CrearProductoRequest request = new CrearProductoRequest();
        request.setEmpresaId(10L);
        request.setSku("CAT-001");
        request.setNombre("Con Categoria");
        request.setCategoriaId(3L);
        request.setPrecioUnitario(BigDecimal.TEN);

        Categoria categoria = new Categoria();
        categoria.setId(3L);
        categoria.setNombre("Electronica");

        when(categoriaRepository.findById(3L)).thenReturn(Optional.of(categoria));
        when(productoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertNotNull(service.crearProducto(request));
        verify(categoriaRepository).findById(3L);
    }

    @Test
    void crearProducto_conDefaults_usaValoresPorDefecto() {
        CrearProductoRequest request = new CrearProductoRequest();
        request.setEmpresaId(10L);
        request.setSku("DEF-001");
        request.setNombre("Producto Defaults");

        when(productoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.crearProducto(request);

        ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);
        verify(productoRepository).save(captor.capture());
        assertEquals(0, captor.getValue().getStockActual());
        assertEquals(0, captor.getValue().getStockMinimo());
        assertEquals("UNIDAD", captor.getValue().getUnidadMedida());
    }

    // ── actualizarProducto ────────────────────────────────────────────────────
    @Test
    void actualizarProducto_todosLosCampos_actualizaCorrectamente() {
        ActualizarProductoRequest request = new ActualizarProductoRequest();
        request.setNombre("Nombre Nuevo");
        request.setDescripcion("Descripcion nueva");
        request.setPrecioUnitario(BigDecimal.valueOf(20.00));
        request.setStockMinimo(10);
        request.setActivo(false);
        request.setStockAgregar(15);
        request.setCategoriaId(5L);

        Categoria categoria = new Categoria();
        categoria.setId(5L);

        when(productoRepository.findById(1L)).thenReturn(Optional.of(productoBase));
        when(categoriaRepository.findById(5L)).thenReturn(Optional.of(categoria));
        when(productoRepository.save(any())).thenReturn(productoBase);

        ProductoResponse response = service.actualizarProducto(1L, request);

        assertNotNull(response);
        assertEquals("Nombre Nuevo", productoBase.getNombre());
        assertEquals("Descripcion nueva", productoBase.getDescripcion());
        assertEquals(10, productoBase.getStockMinimo());
        assertFalse(productoBase.getActivo());
        assertEquals(115, productoBase.getStockActual());
    }

    @Test
    void actualizarProducto_stockAgregarCeroONegativo_noModificaStock() {
        ActualizarProductoRequest request = new ActualizarProductoRequest();
        request.setStockAgregar(0);

        when(productoRepository.findById(1L)).thenReturn(Optional.of(productoBase));
        when(productoRepository.save(any())).thenReturn(productoBase);

        service.actualizarProducto(1L, request);

        assertEquals(100, productoBase.getStockActual()); // sin cambio
    }

    @Test
    void actualizarProducto_noExiste_lanzaExcepcion() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ProductoNoEncontradoException.class,
                () -> service.actualizarProducto(99L, new ActualizarProductoRequest()));
    }

    // ── eliminarProducto ──────────────────────────────────────────────────────
    @Test
    void eliminarProducto_existente_marcaComoInactivo() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(productoBase));
        when(productoRepository.save(any())).thenReturn(productoBase);

        service.eliminarProducto(1L);

        assertFalse(productoBase.getActivo());
    }

    @Test
    void eliminarProducto_noExiste_lanzaExcepcion() {
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ProductoNoEncontradoException.class,
                () -> service.eliminarProducto(99L));
    }
}

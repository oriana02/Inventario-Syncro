package com.syncro.inventario.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncro.inventario.dto.*;
import com.syncro.inventario.exception.ProductoNoEncontradoException;
import com.syncro.inventario.exception.StockInsuficienteException;
import com.syncro.inventario.model.ReservaStock;
import com.syncro.inventario.service.InventarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.syncro.inventario.config.SecurityConfig;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventarioController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@DisplayName("InventarioController - Tests de integracion con MockMvc")
class InventarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InventarioService inventarioService;

    private ProductoResponse productoMock;

    @BeforeEach
    void setUp() {
        productoMock = ProductoResponse.builder()
                .id(1L)
                .empresaId(1L)
                .sku("ELEC-001")
                .nombre("Audifonos Bluetooth")
                .descripcion("Audifonos inalambricos")
                .stockActual(50)
                .stockReservado(0)
                .stockMinimo(10)
                .precioUnitario(new BigDecimal("29990"))
                .activo(true)
                .fechaCreacion(LocalDateTime.now())
                .build();
    }

    // =========================================================================
    // GET /inventario
    // =========================================================================

    @Test
    @DisplayName("GET /inventario - health check retorna 200")
    void healthCheck_retorna200() throws Exception {
        mockMvc.perform(get("/inventario"))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // GET /inventario/productos
    // =========================================================================

    @Test
    @DisplayName("GET /inventario/productos - retorna lista de productos")
    void consultarProductos_retornaLista() throws Exception {
        when(inventarioService.consultarProductos(1L, null))
                .thenReturn(List.of(productoMock));

        mockMvc.perform(get("/inventario/productos")
                .param("empresaId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].sku").value("ELEC-001"))
                .andExpect(jsonPath("$[0].stockActual").value(50));
    }

    @Test
    @DisplayName("GET /inventario/productos - retorna lista vacia si no hay productos")
    void consultarProductos_listaVacia() throws Exception {
        when(inventarioService.consultarProductos(99L, null))
                .thenReturn(List.of());

        mockMvc.perform(get("/inventario/productos")
                .param("empresaId", "99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /inventario/productos - filtra por categoria")
    void consultarProductos_conCategoria() throws Exception {
        when(inventarioService.consultarProductos(1L, 1L))
                .thenReturn(List.of(productoMock));

        mockMvc.perform(get("/inventario/productos")
                .param("empresaId", "1")
                .param("categoriaId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sku").value("ELEC-001"));
    }

    // =========================================================================
    // GET /inventario/productos/{id}
    // =========================================================================

    @Test
    @DisplayName("GET /inventario/productos/{id} - retorna producto existente")
    void consultarProductoPorId_existente_retorna200() throws Exception {
        when(inventarioService.consultarProductoPorId(1L))
                .thenReturn(productoMock);

        mockMvc.perform(get("/inventario/productos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Audifonos Bluetooth"));
    }

    @Test
    @DisplayName("GET /inventario/productos/{id} - producto inexistente retorna 404")
    void consultarProductoPorId_noExiste_retorna404() throws Exception {
        when(inventarioService.consultarProductoPorId(99L))
                .thenThrow(new ProductoNoEncontradoException("Producto no encontrado con ID: 99"));

        mockMvc.perform(get("/inventario/productos/99"))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // PATCH /inventario/descontar
    // =========================================================================

    @Test
    @DisplayName("PATCH /inventario/descontar - descuenta stock correctamente")
    void descontarStock_valido_retorna200() throws Exception {
        DescuentoStockRequest request = DescuentoStockRequest.builder()
                .productoId(1L).pedidoId(5L).cantidad(10).build();

        doNothing().when(inventarioService).descontarStock(any());

        mockMvc.perform(patch("/inventario/descontar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /inventario/descontar - stock insuficiente retorna 500")
    void descontarStock_insuficiente_retornaError() throws Exception {
        DescuentoStockRequest request = DescuentoStockRequest.builder()
                .productoId(1L).pedidoId(5L).cantidad(1000).build();

        doThrow(new StockInsuficienteException("Stock insuficiente"))
                .when(inventarioService).descontarStock(any());

        mockMvc.perform(patch("/inventario/descontar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PATCH /inventario/descontar - body invalido retorna 400")
    void descontarStock_bodyInvalido_retorna400() throws Exception {
        mockMvc.perform(patch("/inventario/descontar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // POST /inventario/ajuste
    // =========================================================================

    @Test
    @DisplayName("POST /inventario/ajuste - ajuste valido retorna 201")
    void realizarAjuste_valido_retorna201() throws Exception {
        AjusteInventarioRequest request = AjusteInventarioRequest.builder()
                .productoId(1L).empresaId(1L).tipoAjuste("INGRESO_MERCADERIA")
                .cantidad(20).motivo("Nuevo lote").usuarioId(1L).build();

        doNothing().when(inventarioService).realizarAjuste(any());

        mockMvc.perform(post("/inventario/ajuste")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /inventario/ajuste - body invalido retorna 400")
    void realizarAjuste_bodyInvalido_retorna400() throws Exception {
        mockMvc.perform(post("/inventario/ajuste")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // POST /inventario/reserva
    // =========================================================================

    @Test
    @DisplayName("POST /inventario/reserva - reserva valida retorna 201")
    void reservarStock_valido_retorna201() throws Exception {
        ReservaStockRequest request = ReservaStockRequest.builder()
                .productoId(1L).pedidoId(5L).cantidad(10).build();

        ReservaStock reserva = ReservaStock.builder()
                .id(1L).pedidoId(5L).cantidad(10).estado("ACTIVA")
                .fechaExpiracion(LocalDateTime.now().plusMinutes(30)).build();

        when(inventarioService.reservarStock(any())).thenReturn(reserva);

        mockMvc.perform(post("/inventario/reserva")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ACTIVA"));
    }

    // =========================================================================
    // POST /inventario/reserva/{id}/liberar
    // =========================================================================

    @Test
    @DisplayName("POST /inventario/reserva/{id}/liberar - libera reserva existente")
    void liberarReserva_existente_retorna200() throws Exception {
        doNothing().when(inventarioService).liberarReserva(1L);

        mockMvc.perform(post("/inventario/reserva/1/liberar"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /inventario/reserva/{id}/liberar - reserva inexistente retorna 404")
    void liberarReserva_noExiste_retorna404() throws Exception {
        doThrow(new ProductoNoEncontradoException("Reserva no encontrada con ID: 99"))
                .when(inventarioService).liberarReserva(99L);

        mockMvc.perform(post("/inventario/reserva/99/liberar"))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // POST /inventario/reserva/{id}/confirmar
    // =========================================================================

    @Test
    @DisplayName("POST /inventario/reserva/{id}/confirmar - confirma reserva existente")
    void confirmarReserva_existente_retorna200() throws Exception {
        doNothing().when(inventarioService).confirmarReserva(1L);

        mockMvc.perform(post("/inventario/reserva/1/confirmar"))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // POST /inventario/productos
    // =========================================================================

    @Test
    @DisplayName("POST /inventario/productos - crea producto valido retorna 201")
    void crearProducto_valido_retorna201() throws Exception {
        CrearProductoRequest request = CrearProductoRequest.builder()
                .empresaId(1L).sku("NUEVO-001").nombre("Nuevo Producto")
                .precioUnitario(new BigDecimal("9990")).build();

        when(inventarioService.crearProducto(any())).thenReturn(productoMock);

        mockMvc.perform(post("/inventario/productos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("ELEC-001"));
    }

    @Test
    @DisplayName("POST /inventario/productos - body invalido retorna 400")
    void crearProducto_bodyInvalido_retorna400() throws Exception {
        mockMvc.perform(post("/inventario/productos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // PATCH /inventario/productos/{id}
    // =========================================================================

    @Test
    @DisplayName("PATCH /inventario/productos/{id} - actualiza producto existente")
    void actualizarProducto_existente_retorna200() throws Exception {
        ActualizarProductoRequest request = ActualizarProductoRequest.builder()
                .nombre("Nuevo Nombre").precioUnitario(new BigDecimal("19990")).build();

        when(inventarioService.actualizarProducto(eq(1L), any())).thenReturn(productoMock);

        mockMvc.perform(patch("/inventario/productos/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    // =========================================================================
    // DELETE /inventario/productos/{id}
    // =========================================================================

    @Test
    @DisplayName("DELETE /inventario/productos/{id} - elimina producto existente")
    void eliminarProducto_existente_retorna204() throws Exception {
        doNothing().when(inventarioService).eliminarProducto(1L);

        mockMvc.perform(delete("/inventario/productos/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /inventario/productos/{id} - producto inexistente retorna 404")
    void eliminarProducto_noExiste_retorna404() throws Exception {
        doThrow(new ProductoNoEncontradoException("Producto no encontrado: 99"))
                .when(inventarioService).eliminarProducto(99L);

        mockMvc.perform(delete("/inventario/productos/99"))
                .andExpect(status().isNotFound());
    }
}

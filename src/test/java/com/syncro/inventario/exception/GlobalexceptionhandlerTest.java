package com.syncro.inventario.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    // ── handleNotFound ────────────────────────────────────────────────────────
    @Test
    void handleNotFound_retornaStatus404() {
        ProductoNoEncontradoException ex = new ProductoNoEncontradoException("Producto no encontrado con ID: 1");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("Producto no encontrado con ID: 1", response.getBody().getMessage());
        assertNotNull(response.getBody().getTimestamp());
    }

    // ── handleStockInsuficiente ───────────────────────────────────────────────
    @Test
    void handleStockInsuficiente_retornaStatus409() {
        StockInsuficienteException ex = new StockInsuficienteException("Stock insuficiente para SKU: ABC");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleStockInsuficiente(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().getStatus());
        assertEquals("Stock insuficiente para SKU: ABC", response.getBody().getMessage());
    }

    // ── handleIllegalState ────────────────────────────────────────────────────
    @Test
    void handleIllegalState_retornaStatus409() {
        IllegalStateException ex = new IllegalStateException("La reserva no está en estado ACTIVA");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleIllegalState(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().getStatus());
        assertEquals("La reserva no está en estado ACTIVA", response.getBody().getMessage());
    }

    // ── handleValidation ─────────────────────────────────────────────────────
    @Test
    void handleValidation_retornaStatus400ConErrores() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        FieldError fieldError = new FieldError("request", "nombre", "no debe estar vacío");
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Error de validacion en los campos enviados", response.getBody().getMessage());
        assertNotNull(response.getBody().getErrors());
        assertTrue(response.getBody().getErrors().containsKey("nombre"));
        assertEquals("no debe estar vacío", response.getBody().getErrors().get("nombre"));
    }

    @Test
    void handleValidation_sinCampos_retornaMapaVacio() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(List.of());

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().getErrors().isEmpty());
    }

    // ── handleGeneral ─────────────────────────────────────────────────────────
    @Test
    void handleGeneral_retornaStatus500() {
        Exception ex = new RuntimeException("Error inesperado");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response = handler.handleGeneral(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("Error interno del servidor", response.getBody().getMessage());
    }

    // ── ErrorResponse.of ─────────────────────────────────────────────────────
    @Test
    void errorResponseOf_construyeCorrectamente() {
        GlobalExceptionHandler.ErrorResponse response
                = GlobalExceptionHandler.ErrorResponse.of(404, "No encontrado");

        assertEquals(404, response.getStatus());
        assertEquals("No encontrado", response.getMessage());
        assertNotNull(response.getTimestamp());
        assertNull(response.getErrors());
    }
}

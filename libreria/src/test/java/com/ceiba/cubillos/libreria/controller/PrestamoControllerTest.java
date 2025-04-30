package com.ceiba.cubillos.libreria.controller;

import com.fasterxml.jackson.databind.ObjectMapper; // Para convertir objetos a JSON
import com.ceiba.cubillos.libreria.dto.*;
import com.ceiba.cubillos.libreria.entity.Prestamo;
import com.ceiba.cubillos.libreria.enums.TipoUsuario;
import com.ceiba.cubillos.libreria.exception.ResourceNotFoundException;
import com.ceiba.cubillos.libreria.exception.TipoUsuarioNoPermitidoException;
import com.ceiba.cubillos.libreria.exception.UsuarioInvitadoException;
import com.ceiba.cubillos.libreria.service.PrestamoService;
import com.ceiba.cubillos.libreria.util.DateUtils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest; // Testea sólo la capa web
import org.springframework.boot.test.mock.mockito.MockBean; // Crea un mock del servicio en el contexto
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc; // Para simular peticiones HTTP
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given; // Estilo BDD para when()
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.CoreMatchers.is; // Para aserciones en JSON Path

@WebMvcTest(PrestamoController.class) // Carga sólo el contexto necesario para PrestamoController
class PrestamoControllerTest {

    @Autowired
    private MockMvc mockMvc; // Objeto para realizar peticiones HTTP simuladas

    @MockBean // Reemplaza el bean real de PrestamoService por un mock
    private PrestamoService prestamoService;

    @Autowired
    private ObjectMapper objectMapper; // Para convertir objetos Java a JSON y viceversa

    @Test
    @DisplayName("POST /prestamo - Debería crear préstamo y devolver 200 OK")
    void crearPrestamo_Valido_Retorna200() throws Exception {
        // Arrange
        PrestamoRequest request = new PrestamoRequest("ISBN123456", "USERAFI01", TipoUsuario.AFILIADO.getId());
        LocalDate fechaDev = DateUtils.addBusinessDaysSkippingWeekends(LocalDate.now(), TipoUsuario.AFILIADO.getDiasMaximoPrestamo());
        PrestamoResponse responseDto = PrestamoResponse.builder()
                .id(1L)
                .fechaMaximaDevolucion(DateUtils.formatLocalDate(fechaDev))
                .build();

        // Configura el mock del servicio: cuando se llame a crearPrestamo con cualquier request, devuelve responseDto
        given(prestamoService.crearPrestamo(any(PrestamoRequest.class))).willReturn(responseDto);

        // Act: Realiza la petición POST simulada
        ResultActions result = mockMvc.perform(post("/prestamo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))); // Convierte el request a JSON

        // Assert: Verifica la respuesta
        result.andExpect(status().isOk()) // Espera HTTP 200 OK
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(responseDto.getId().intValue()))) // Verifica el campo 'id' en el JSON
                .andExpect(jsonPath("$.fechaMaximaDevolucion", is(responseDto.getFechaMaximaDevolucion()))); // Verifica la fecha
    }

    @Test
    @DisplayName("POST /prestamo - Usuario invitado ya tiene libro - Debería devolver 400 Bad Request")
    void crearPrestamo_InvitadoRepetido_Retorna400() throws Exception {
        // Arrange
        PrestamoRequest request = new PrestamoRequest("ISBNINV002", "USERINV01", TipoUsuario.INVITADO.getId());
        String errorMessage = String.format("El usuario con identificación %s ya tiene un libro prestado...", request.identificacionUsuario());

        // Configura el mock para que lance la excepción esperada
        given(prestamoService.crearPrestamo(any(PrestamoRequest.class)))
                .willThrow(new UsuarioInvitadoException(errorMessage));

        // Act
        ResultActions result = mockMvc.perform(post("/prestamo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Assert
        result.andExpect(status().isBadRequest()) // Espera HTTP 400
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.mensaje", is(errorMessage))); // Verifica el mensaje de error
    }

    @Test
    @DisplayName("POST /prestamo - Tipo de usuario inválido - Debería devolver 400 Bad Request")
    void crearPrestamo_TipoInvalido_Retorna400() throws Exception {
        // Arrange
        PrestamoRequest request = new PrestamoRequest("ISBNERR99", "USERERR01", 99);
        String errorMessage = "Tipo de usuario no permitido en la biblioteca";

        given(prestamoService.crearPrestamo(any(PrestamoRequest.class)))
                .willThrow(new TipoUsuarioNoPermitidoException(errorMessage));

        // Act
        ResultActions result = mockMvc.perform(post("/prestamo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.mensaje", is(errorMessage)));
    }

    @Test
    @DisplayName("POST /prestamo - Request inválido (campos vacíos) - Debería devolver 400 Bad Request")
    void crearPrestamo_RequestInvalido_Retorna400() throws Exception {
        // Arrange
        PrestamoRequest request = new PrestamoRequest("", null, null); // Datos inválidos según @NotBlank/@NotNull

        // No necesitamos mockear el servicio aquí, la validación ocurre antes
        // Act
        ResultActions result = mockMvc.perform(post("/prestamo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Assert
        result.andExpect(status().isBadRequest()) // Espera 400 por validación
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                // Verifica que existan errores para los campos esperados
                .andExpect(jsonPath("$.isbn").exists())
                .andExpect(jsonPath("$.identificacionUsuario").exists())
                .andExpect(jsonPath("$.tipoUsuario").exists());
    }

    @Test
    @DisplayName("GET /prestamo/{id} - ID existente - Debería devolver 200 OK con datos")
    void obtenerPrestamo_IdExistente_Retorna200() throws Exception {
        // Arrange
        Long prestamoId = 5L;
        LocalDate fechaDev = DateUtils.addBusinessDaysSkippingWeekends(LocalDate.now(), TipoUsuario.EMPLEADO.getDiasMaximoPrestamo());
        PrestamoResponse responseDto = PrestamoResponse.builder()
                .id(prestamoId)
                .isbn("ABCDEF1234")
                .identificacionUsuario("EMP001")
                .tipoUsuario(TipoUsuario.EMPLEADO.getId())
                .fechaMaximaDevolucion(DateUtils.formatLocalDate(fechaDev))
                .build();

        given(prestamoService.obtenerPrestamoPorId(prestamoId)).willReturn(responseDto);

        // Act
        ResultActions result = mockMvc.perform(get("/prestamo/{id}", prestamoId) // Usa la variable {id}
                .accept(MediaType.APPLICATION_JSON)); // Indica que esperamos JSON

        // Assert
        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(prestamoId.intValue())))
                .andExpect(jsonPath("$.isbn", is(responseDto.getIsbn())))
                .andExpect(jsonPath("$.identificacionUsuario", is(responseDto.getIdentificacionUsuario())))
                .andExpect(jsonPath("$.tipoUsuario", is(responseDto.getTipoUsuario())))
                .andExpect(jsonPath("$.fechaMaximaDevolucion", is(responseDto.getFechaMaximaDevolucion())));
    }

    @Test
    @DisplayName("GET /prestamo/{id} - ID no existente - Debería devolver 404 Not Found")
    void obtenerPrestamo_IdNoExistente_Retorna404() throws Exception {
        // Arrange
        Long prestamoId = 999L;
        String errorMessage = "Préstamo no encontrado con id: " + prestamoId;

        given(prestamoService.obtenerPrestamoPorId(prestamoId))
                .willThrow(new ResourceNotFoundException(errorMessage));

        // Act
        ResultActions result = mockMvc.perform(get("/prestamo/{id}", prestamoId)
                .accept(MediaType.APPLICATION_JSON));

        // Assert
        result.andExpect(status().isNotFound()) // Espera 404
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.mensaje", is(errorMessage)));
    }
}

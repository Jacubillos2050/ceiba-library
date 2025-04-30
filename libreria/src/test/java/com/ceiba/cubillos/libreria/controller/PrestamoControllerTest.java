package com.ceiba.cubillos.libreria.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.CoreMatchers.is;

@WebMvcTest(PrestamoController.class)
class PrestamoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PrestamoService prestamoService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /prestamo - Debería crear préstamo y devolver 200 OK")
    void crearPrestamo_Valido_Retorna200() throws Exception {
        
        PrestamoRequest request = new PrestamoRequest("ISBN123456", "USERAFI01", TipoUsuario.AFILIADO.getId());
        LocalDate fechaDev = DateUtils.addBusinessDaysSkippingWeekends(LocalDate.now(), TipoUsuario.AFILIADO.getDiasMaximoPrestamo());
        PrestamoResponse responseDto = PrestamoResponse.builder()
                .id(1L)
                .fechaMaximaDevolucion(DateUtils.formatLocalDate(fechaDev))
                .build();

        // Configura el mock del servicio
        given(prestamoService.crearPrestamo(any(PrestamoRequest.class))).willReturn(responseDto);

        // Petición POST simulada
        ResultActions result = mockMvc.perform(post("/prestamo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Verificar la respuesta
        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(responseDto.getId().intValue())))
                .andExpect(jsonPath("$.fechaMaximaDevolucion", is(responseDto.getFechaMaximaDevolucion())));
    }

    @Test
    @DisplayName("POST /prestamo - Usuario invitado ya tiene libro - Debería devolver 400 Bad Request")
    void crearPrestamo_InvitadoRepetido_Retorna400() throws Exception {
        
        PrestamoRequest request = new PrestamoRequest("ISBNINV002", "USERINV01", TipoUsuario.INVITADO.getId());
        String errorMessage = String.format("El usuario con identificación %s ya tiene un libro prestado...", request.identificacionUsuario());

        given(prestamoService.crearPrestamo(any(PrestamoRequest.class)))
                .willThrow(new UsuarioInvitadoException(errorMessage));

        
        ResultActions result = mockMvc.perform(post("/prestamo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        
        result.andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.mensaje", is(errorMessage)));
    }

    @Test
    @DisplayName("POST /prestamo - Tipo de usuario inválido - Debería devolver 400 Bad Request")
    void crearPrestamo_TipoInvalido_Retorna400() throws Exception {
        
        PrestamoRequest request = new PrestamoRequest("ISBNERR99", "USERERR01", 99);
        String errorMessage = "Tipo de usuario no permitido en la biblioteca";

        given(prestamoService.crearPrestamo(any(PrestamoRequest.class)))
                .willThrow(new TipoUsuarioNoPermitidoException(errorMessage));

        
        ResultActions result = mockMvc.perform(post("/prestamo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        
        result.andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.mensaje", is(errorMessage)));
    }

    @Test
    @DisplayName("POST /prestamo - Request inválido (campos vacíos) - Debería devolver 400 Bad Request")
    void crearPrestamo_RequestInvalido_Retorna400() throws Exception {
        
        PrestamoRequest request = new PrestamoRequest("", null, null);

        ResultActions result = mockMvc.perform(post("/prestamo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        result.andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.isbn").exists())
                .andExpect(jsonPath("$.identificacionUsuario").exists())
                .andExpect(jsonPath("$.tipoUsuario").exists());
    }

    @Test
    @DisplayName("GET /prestamo/{id} - ID existente - Debería devolver 200 OK con datos")
    void obtenerPrestamo_IdExistente_Retorna200() throws Exception {
        
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

        
        ResultActions result = mockMvc.perform(get("/prestamo/{id}", prestamoId)
                .accept(MediaType.APPLICATION_JSON));

        
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
        
        Long prestamoId = 999L;
        String errorMessage = "Préstamo no encontrado con id: " + prestamoId;

        given(prestamoService.obtenerPrestamoPorId(prestamoId))
                .willThrow(new ResourceNotFoundException(errorMessage));

        ResultActions result = mockMvc.perform(get("/prestamo/{id}", prestamoId)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.mensaje", is(errorMessage)));
    }
}

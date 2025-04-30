package com.ceiba.cubillos.libreria.service.impl;

import com.ceiba.cubillos.libreria.dto.*;
import com.ceiba.cubillos.libreria.entity.Prestamo;
import com.ceiba.cubillos.libreria.enums.TipoUsuario;
import com.ceiba.cubillos.libreria.exception.ResourceNotFoundException;
import com.ceiba.cubillos.libreria.exception.TipoUsuarioNoPermitidoException;
import com.ceiba.cubillos.libreria.exception.UsuarioInvitadoException;
import com.ceiba.cubillos.libreria.repository.PrestamoRepository;
import com.ceiba.cubillos.libreria.service.PrestamoService;
import com.ceiba.cubillos.libreria.service.impl.PrestamoServiceImpl;
import com.ceiba.cubillos.libreria.util.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrestamoServiceImplTest {

    @Mock
    private PrestamoRepository prestamoRepository;

    @InjectMocks
    private PrestamoServiceImpl prestamoService;

    private PrestamoRequest requestAfiliado;
    private PrestamoRequest requestEmpleado;
    private PrestamoRequest requestInvitado;
    private PrestamoRequest requestInvitadoRepetido;
    private PrestamoRequest requestTipoInvalido;
    private Prestamo prestamoGuardado;

    @BeforeEach
    void setUp() {
        
        requestAfiliado = new PrestamoRequest("ABCD123456", "USER001", TipoUsuario.AFILIADO.getId());
        requestEmpleado = new PrestamoRequest("BCDE234567", "USER002", TipoUsuario.EMPLEADO.getId());
        requestInvitado = new PrestamoRequest("CDEF345678", "USER003", TipoUsuario.INVITADO.getId());
        requestInvitadoRepetido = new PrestamoRequest("DEFG456789", "USER003", TipoUsuario.INVITADO.getId());
        requestTipoInvalido = new PrestamoRequest("EFGH567890", "USER004", 99);

        prestamoGuardado = Prestamo.builder()
                .id(1L)
                .isbn(requestAfiliado.isbn())
                .identificacionUsuario(requestAfiliado.identificacionUsuario())
                .tipoUsuario(TipoUsuario.AFILIADO)
                .fechaMaximaDevolucion(LocalDate.now().plusDays(10))
                .build();
    }

    @Test
    @DisplayName("Debería crear préstamo para usuario AFILIADO correctamente")
    void crearPrestamo_Afiliado_Exitoso() {
        
        when(prestamoRepository.save(any(Prestamo.class))).thenReturn(prestamoGuardado);

        PrestamoResponse response = prestamoService.crearPrestamo(requestAfiliado);

        
        assertNotNull(response);
        assertEquals(prestamoGuardado.getId(), response.getId());
        assertNotNull(response.getFechaMaximaDevolucion());
        
        LocalDate expectedDate = DateUtils.addBusinessDaysSkippingWeekends(LocalDate.now(), TipoUsuario.AFILIADO.getDiasMaximoPrestamo());
        assertEquals(DateUtils.formatLocalDate(expectedDate), response.getFechaMaximaDevolucion());

        verify(prestamoRepository, times(1)).save(any(Prestamo.class));
        verify(prestamoRepository, never()).existsByIdentificacionUsuarioAndTipoUsuario(anyString(), any(TipoUsuario.class));
    }

    @Test
    @DisplayName("Debería crear préstamo para usuario EMPLEADO correctamente")
    void crearPrestamo_Empleado_Exitoso() {
        prestamoGuardado.setTipoUsuario(TipoUsuario.EMPLEADO);
        when(prestamoRepository.save(any(Prestamo.class))).thenReturn(prestamoGuardado);

        PrestamoResponse response = prestamoService.crearPrestamo(requestEmpleado);

        assertNotNull(response);
        LocalDate expectedDate = DateUtils.addBusinessDaysSkippingWeekends(LocalDate.now(), TipoUsuario.EMPLEADO.getDiasMaximoPrestamo());
        assertEquals(DateUtils.formatLocalDate(expectedDate), response.getFechaMaximaDevolucion());
        verify(prestamoRepository, times(1)).save(any(Prestamo.class));
        verify(prestamoRepository, never()).existsByIdentificacionUsuarioAndTipoUsuario(anyString(), any(TipoUsuario.class));
    }

    @Test
    @DisplayName("Debería crear préstamo para usuario INVITADO si no tiene préstamos previos")
    void crearPrestamo_Invitado_SinPrestamoPrevio_Exitoso() {
        prestamoGuardado.setTipoUsuario(TipoUsuario.INVITADO);

        when(prestamoRepository.existsByIdentificacionUsuarioAndTipoUsuario(
                requestInvitado.identificacionUsuario(), TipoUsuario.INVITADO))
                .thenReturn(false);
        when(prestamoRepository.save(any(Prestamo.class))).thenReturn(prestamoGuardado);

        PrestamoResponse response = prestamoService.crearPrestamo(requestInvitado);

        assertNotNull(response);
        LocalDate expectedDate = DateUtils.addBusinessDaysSkippingWeekends(LocalDate.now(), TipoUsuario.INVITADO.getDiasMaximoPrestamo());
        assertEquals(DateUtils.formatLocalDate(expectedDate), response.getFechaMaximaDevolucion());
        verify(prestamoRepository, times(1)).existsByIdentificacionUsuarioAndTipoUsuario(requestInvitado.identificacionUsuario(), TipoUsuario.INVITADO);
        verify(prestamoRepository, times(1)).save(any(Prestamo.class));
    }

    @Test
    @DisplayName("Debería lanzar UsuarioInvitadoException si el usuario INVITADO ya tiene un préstamo")
    void crearPrestamo_Invitado_ConPrestamoPrevio_LanzaExcepcion() {
        when(prestamoRepository.existsByIdentificacionUsuarioAndTipoUsuario(
                requestInvitadoRepetido.identificacionUsuario(), TipoUsuario.INVITADO))
                .thenReturn(true);

        UsuarioInvitadoException exception = assertThrows(UsuarioInvitadoException.class, () -> {
            prestamoService.crearPrestamo(requestInvitadoRepetido);
        });

        assertTrue(exception.getMessage().contains(requestInvitadoRepetido.identificacionUsuario()));
        assertTrue(exception.getMessage().contains("ya tiene un libro prestado"));

        verify(prestamoRepository, never()).save(any(Prestamo.class));
        verify(prestamoRepository, times(1)).existsByIdentificacionUsuarioAndTipoUsuario(requestInvitadoRepetido.identificacionUsuario(), TipoUsuario.INVITADO);
    }

    @Test
    @DisplayName("Debería lanzar TipoUsuarioNoPermitidoException si el tipo de usuario es inválido")
    void crearPrestamo_TipoUsuarioInvalido_LanzaExcepcion() {
        
        TipoUsuarioNoPermitidoException exception = assertThrows(TipoUsuarioNoPermitidoException.class, () -> {
            prestamoService.crearPrestamo(requestTipoInvalido);
        });

        assertEquals("Tipo de usuario no permitido en la biblioteca", exception.getMessage());

        verifyNoInteractions(prestamoRepository);
    }

    @Test
    @DisplayName("Debería obtener préstamo por ID existente")
    void obtenerPrestamoPorId_Existente_RetornaPrestamo() {
        
        Long idExistente = 1L;
        when(prestamoRepository.findById(idExistente)).thenReturn(Optional.of(prestamoGuardado));
        PrestamoResponse response = prestamoService.obtenerPrestamoPorId(idExistente);

        assertNotNull(response);
        assertEquals(prestamoGuardado.getId(), response.getId());
        assertEquals(prestamoGuardado.getIsbn(), response.getIsbn());
        assertEquals(prestamoGuardado.getIdentificacionUsuario(), response.getIdentificacionUsuario());
        assertEquals(prestamoGuardado.getTipoUsuario().getId(), response.getTipoUsuario());
        assertEquals(DateUtils.formatLocalDate(prestamoGuardado.getFechaMaximaDevolucion()), response.getFechaMaximaDevolucion());

        verify(prestamoRepository, times(1)).findById(idExistente);
    }

    @Test
    @DisplayName("Debería lanzar ResourceNotFoundException al obtener préstamo por ID no existente")
    void obtenerPrestamoPorId_NoExistente_LanzaExcepcion() {
        
        Long idNoExistente = 999L;
        when(prestamoRepository.findById(idNoExistente)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            prestamoService.obtenerPrestamoPorId(idNoExistente);
        });

        assertEquals("Préstamo no encontrado con id: " + idNoExistente, exception.getMessage());
        verify(prestamoRepository, times(1)).findById(idNoExistente);
    }
}

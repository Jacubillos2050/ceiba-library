package com.ceiba.cubillos.libreria.service.impl;

import com.ceiba.cubillos.libreria.dto.*;
import com.ceiba.cubillos.libreria.entity.Prestamo;
import com.ceiba.cubillos.libreria.enums.*;
import com.ceiba.cubillos.libreria.exception.ResourceNotFoundException;
import com.ceiba.cubillos.libreria.exception.TipoUsuarioNoPermitidoException;
import com.ceiba.cubillos.libreria.exception.UsuarioInvitadoException;
import com.ceiba.cubillos.libreria.repository.PrestamoRepository;
import com.ceiba.cubillos.libreria.service.PrestamoService;
import com.ceiba.cubillos.libreria.util.DateUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class PrestamoServiceImpl implements PrestamoService {

    private final PrestamoRepository prestamoRepository;

    @Override
    @Transactional
    public PrestamoResponse crearPrestamo(PrestamoRequest request) {
        TipoUsuario tipoUsuario;
        try {
            // Validar y obtener el Enum TipoUsuario
            tipoUsuario = TipoUsuario.fromId(request.tipoUsuario());
        } catch (IllegalArgumentException e) {
            throw new TipoUsuarioNoPermitidoException("Tipo de usuario no permitido en la biblioteca");
        }

        // Validar si es usuario invitado
        if (tipoUsuario == TipoUsuario.INVITADO) {
            boolean yaTienePrestamo = prestamoRepository.existsByIdentificacionUsuarioAndTipoUsuario(
                    request.identificacionUsuario(), TipoUsuario.INVITADO);
            if (yaTienePrestamo) {
                throw new UsuarioInvitadoException(
                        String.format("El usuario con identificación %s ya tiene un libro prestado por lo cual no se le puede realizar otro préstamo",
                                request.identificacionUsuario())
                );
            }
        }

        // Calcular fecha máxima de devolución
        LocalDate fechaMaximaDevolucion = calcularFechaDevolucion(tipoUsuario);

        // Crear la entidad Prestamo
        Prestamo nuevoPrestamo = Prestamo.builder()
                .isbn(request.isbn())
                .identificacionUsuario(request.identificacionUsuario())
                .tipoUsuario(tipoUsuario) // Guardamos el Enum
                .fechaMaximaDevolucion(fechaMaximaDevolucion)
                .build();

        // Guardar en la base de datos
        Prestamo prestamoGuardado = prestamoRepository.save(nuevoPrestamo);

        // Mapear a DTO de respuesta
        return PrestamoResponse.builder()
                .id(prestamoGuardado.getId())
                .fechaMaximaDevolucion(DateUtils.formatLocalDate(prestamoGuardado.getFechaMaximaDevolucion()))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PrestamoResponse obtenerPrestamoPorId(Long id) {
        Prestamo prestamo = prestamoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Préstamo no encontrado con id: " + id));

        return PrestamoResponse.builder()
                .id(prestamo.getId())
                .isbn(prestamo.getIsbn())
                .identificacionUsuario(prestamo.getIdentificacionUsuario())
                .tipoUsuario(prestamo.getTipoUsuario().getId()) // Devolvemos el ID numérico
                .fechaMaximaDevolucion(DateUtils.formatLocalDate(prestamo.getFechaMaximaDevolucion()))
                .build();
    }

    private LocalDate calcularFechaDevolucion(TipoUsuario tipoUsuario) {
        LocalDate fechaActual = LocalDate.now();
        int diasASumar = tipoUsuario.getDiasMaximoPrestamo();
        return DateUtils.addBusinessDaysSkippingWeekends(fechaActual, diasASumar);
    }
}

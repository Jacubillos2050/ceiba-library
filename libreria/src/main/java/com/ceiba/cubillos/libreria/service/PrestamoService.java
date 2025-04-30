package com.ceiba.cubillos.libreria.service;

import com.ceiba.cubillos.libreria.dto.PrestamoRequest;
import com.ceiba.cubillos.libreria.dto.PrestamoResponse;

public interface PrestamoService {

    /**
     * @param request
     * @return
     */
    PrestamoResponse crearPrestamo(PrestamoRequest request);

    /**
     * @param id
     * @return
     * @throws com.tuempresa.biblioteca.exception.ResourceNotFoundException
     */
    PrestamoResponse obtenerPrestamoPorId(Long id);
}

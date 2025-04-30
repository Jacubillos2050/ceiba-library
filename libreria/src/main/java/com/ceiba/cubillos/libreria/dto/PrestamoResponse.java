package com.ceiba.cubillos.libreria.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class PrestamoResponse {

    private Long id;
    private String fechaMaximaDevolucion;

    private String isbn;
    private String identificacionUsuario;
    private Integer tipoUsuario;
}

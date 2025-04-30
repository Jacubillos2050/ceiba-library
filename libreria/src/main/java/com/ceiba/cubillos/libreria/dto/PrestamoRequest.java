package com.ceiba.cubillos.libreria.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PrestamoRequest(
        @NotBlank(message = "El usuario es obligatorio")
        @Size(max = 10, message = "El usuario no puede tener más de 10 caracteres")
        String isbn,
        @NotBlank(message = "La identificación del usuario es obligatoria")
        @Size(max = 10, message = "La identificación del usuario no puede tener más de 10 caracteres")
        String identificacionUsuario,
        @NotNull(message = "El tipo de usuario es obligatorio")

        Integer tipoUsuario
        ) {

}

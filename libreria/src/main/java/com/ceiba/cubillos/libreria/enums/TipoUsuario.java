package com.ceiba.cubillos.libreria.enums;

import lombok.Getter;

@Getter
public enum TipoUsuario {
    AFILIADO(1, 10),
    EMPLEADO(2, 8),
    INVITADO(3, 7);

    private final int id;
    private final int diasMaximoPrestamo;

    TipoUsuario(int id, int diasMaximoPrestamo) {
        this.id = id;
        this.diasMaximoPrestamo = diasMaximoPrestamo;
    }

    public static TipoUsuario fromId(int id) {
        for (TipoUsuario tipo : values()) {
            if (tipo.id == id) {
                return tipo;
            }
        }

        throw new IllegalArgumentException("Tipo de usuario no válido: " + id);
    }
}

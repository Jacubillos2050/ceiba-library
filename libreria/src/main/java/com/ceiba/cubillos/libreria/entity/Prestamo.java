package com.ceiba.cubillos.libreria.entity;

import java.time.LocalDate;

import com.ceiba.cubillos.libreria.enums.TipoUsuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "prestamos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prestamo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String isbn;

    @Column(nullable = false, length = 10)
    private String identificacionUsuario;

    @Column(nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private TipoUsuario tipoUsuario;

    @Column(nullable = false)
    private LocalDate fechaMaximaDevolucion;

}

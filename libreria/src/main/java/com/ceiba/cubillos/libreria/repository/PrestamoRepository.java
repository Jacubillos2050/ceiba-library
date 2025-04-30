package com.ceiba.cubillos.libreria.repository;

import com.ceiba.cubillos.libreria.entity.Prestamo;
import com.ceiba.cubillos.libreria.enums.TipoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {

    boolean existsByIdentificacionUsuarioAndTipoUsuario(String identificacionUsuario, TipoUsuario tipoUsuario);

    @Override
    Optional<Prestamo> findById(Long id);
}

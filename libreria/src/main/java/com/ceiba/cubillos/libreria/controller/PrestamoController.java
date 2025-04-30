package com.ceiba.cubillos.libreria.controller;

import com.ceiba.cubillos.libreria.dto.*;
import com.ceiba.cubillos.libreria.service.PrestamoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/prestamo")
@RequiredArgsConstructor
public class PrestamoController {

    private final PrestamoService prestamoService;

    @PostMapping
    public ResponseEntity<PrestamoResponse> crearPrestamo(@Valid @RequestBody PrestamoRequest prestamoRequest) {

        PrestamoResponse response = prestamoService.crearPrestamo(prestamoRequest);

        return ResponseEntity.ok(response);

    }

    @GetMapping("/{id}")
    public ResponseEntity<PrestamoResponse> obtenerPrestamo(@PathVariable Long id) {
        PrestamoResponse response = prestamoService.obtenerPrestamoPorId(id);
        return ResponseEntity.ok(response);
    }
}

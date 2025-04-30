package com.ceiba.cubillos.libreria.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import com.ceiba.cubillos.libreria.dto.ErrorResponse;

@ControllerAdvice // Captura excepciones de todos los @Controllers
public class GlobalExceptionHandler {

    // Manejador para nuestras excepciones de negocio específicas que devuelven 400
    @ExceptionHandler({UsuarioInvitadoException.class, TipoUsuarioNoPermitidoException.class})
    public ResponseEntity<ErrorResponse> handleBadRequestBusinessExceptions(BusinessLogicException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST); // 400
    }

    // Manejador para recurso no encontrado (GET /prestamo/{id})
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND); // 404
    }

    // Manejador para errores de validación de DTOs (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error
                -> errors.put(error.getField(), error.getDefaultMessage()));
        // Podrías devolver un ErrorResponse si prefieres un formato único
        // ErrorResponse errorResponse = new ErrorResponse(errors.toString());
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST); // 400
    }

    // Manejador genérico para otras excepciones no capturadas (Error 500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, WebRequest request) {
        // Loggear el error es importante aquí
        System.err.println("Error inesperado: " + ex.getMessage());
        ex.printStackTrace(); // En un sistema real, usar un Logger (SLF4j)

        ErrorResponse errorResponse = new ErrorResponse("Ocurrió un error interno en el servidor.");
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR); // 500
    }
}

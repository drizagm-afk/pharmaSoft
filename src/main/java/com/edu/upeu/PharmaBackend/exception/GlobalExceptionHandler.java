package com.edu.upeu.PharmaBackend.exception;

import com.edu.upeu.PharmaBackend.exception.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ErrorResponseDTO> handleAuthentication(
            org.springframework.security.core.AuthenticationException ex, HttpServletRequest request) {
        return respuesta(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos", request, null);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handleAccessDenied(
            org.springframework.security.access.AccessDeniedException ex, HttpServletRequest request) {
        return respuesta(HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta operación", request, null);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleConflict(
            org.springframework.dao.DataIntegrityViolationException ex, HttpServletRequest request) {
        return respuesta(HttpStatus.CONFLICT, "La operación entra en conflicto con un registro existente", request, null);
    }
    @ExceptionHandler(RecursosNoEncontradosException.class)
    public ResponseEntity<ErrorResponseDTO> handleNotFound(
            RecursosNoEncontradosException ex, HttpServletRequest request) {
        return respuesta(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorResponseDTO> handleBusinessRule(
            ReglaNegocioException ex, HttpServletRequest request) {
        return respuesta(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errores.put(error.getField(), error.getDefaultMessage()));
        return respuesta(HttpStatus.BAD_REQUEST, "Existen errores de validación", request, errores);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidJson(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        return respuesta(HttpStatus.BAD_REQUEST,
                "El cuerpo de la solicitud no es válido. Revise los tipos de datos; los identificadores y las cantidades deben ser enteros.",
                request, null);
    }

    private ResponseEntity<ErrorResponseDTO> respuesta(HttpStatus estado, String mensaje,
            HttpServletRequest request, Map<String, String> errores) {
        var error = new ErrorResponseDTO(LocalDateTime.now(), estado.value(), estado.getReasonPhrase(),
                mensaje, request.getRequestURI(), errores);
        return ResponseEntity.status(estado).body(error);
    }
}

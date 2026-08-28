package com.edu.upeu.PharmaBackend.exception;

import com.edu.upeu.PharmaBackend.dto.ErrorResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

public class GlobalExceptionHandler {
    @ExceptionHandler(RecursosNoEncontradosException.class)
    public ResponseEntity<ErrorResponseDTO> handleRecursosNoEncontradosException(RecursosNoEncontradosException ex){
        return new ResponseEntity<>(new ErrorResponseDTO(
                HttpStatus.NOT_FOUND,
                "Resource Not Found",
                ex.getMessage()
        ), HttpStatus.NOT_FOUND);
    }
}
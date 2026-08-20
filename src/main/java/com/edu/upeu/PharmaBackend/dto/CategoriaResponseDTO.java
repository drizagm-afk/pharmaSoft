package com.edu.upeu.PharmaBackend.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoriaResponseDTO {
    private Long id;
    private String nombre;

    private String descripcion;
    private Boolean estado;

    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaModificacion;
}

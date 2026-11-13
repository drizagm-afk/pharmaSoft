package com.edu.upeu.PharmaBackend.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoriaResponseDTO implements Serializable {
    private String creadoPor;
    private String modificadoPor;
    private static final long serialVersionUID = 1L;
    private Long id;
    private String nombre;

    private String descripcion;
    private Boolean estado;

    //TIMESTAMPS
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaModificacion;
}

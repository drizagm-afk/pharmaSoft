package com.edu.upeu.PharmaBackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductoResponseDTO {
    private Long id;
    private String nombre;
    private String descripcion;

    private BigDecimal precio;
    private Integer stock;

    private Long categoriaId;
    private String categoriaNombre;

    private Boolean estado;

    //TIMESTAMPS
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaModificacion;
}

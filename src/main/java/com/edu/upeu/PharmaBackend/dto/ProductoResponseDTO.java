package com.edu.upeu.PharmaBackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductoResponseDTO implements Serializable {
    private String creadoPor;
    private String modificadoPor;
    private static final long serialVersionUID = 1L;
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

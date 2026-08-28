package com.edu.upeu.PharmaBackend.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductoRequestDTO {
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min=3, max=50, message = "El nombre debe tener entre 3 a 50 carácteres")
    private String nombre;

    @Size(max=200, message = "La descripcion no debe superar los 200 carácteres")
    private String descripcion;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0", inclusive = false, message = "El precio debe ser mayor a 0")
    private BigDecimal precio;

    @NotNull(message = "El stock es obligatorio")
    @Min(value = 0, message = "El stock debe ser mayor o igual a 0")
    private Integer stock;

    @NotNull(message = "La categoria es obligatoria")
    private Long categoriaId;

    @NotNull(message = "El estado es obligatorio")
    private Boolean estado;
}

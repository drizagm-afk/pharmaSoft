package com.edu.upeu.PharmaBackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoriaRequestDTO {
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min=3, max=50, message = "El nombre debe tener entre 3 a 50 carácteres")
    private String nombre;

    @Size(max=200, message = "La descripcion no debe superar los 200 carácteres")
    private String descripcion;

    @NotNull(message = "El estado es obligatorio")
    private Boolean estado;
}

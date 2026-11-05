package com.edu.upeu.PharmaBackend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CambioPasswordRequestDTO(
        @NotBlank(message = "La contraseña actual es obligatoria") String passwordActual,
        @NotBlank(message = "La contraseña nueva es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                message = "La contraseña debe contener al menos una letra y un número") String passwordNueva) {
}

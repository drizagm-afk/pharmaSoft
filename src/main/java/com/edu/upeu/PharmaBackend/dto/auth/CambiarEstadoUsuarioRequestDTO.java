package com.edu.upeu.PharmaBackend.dto.auth;

import jakarta.validation.constraints.NotNull;

public record CambiarEstadoUsuarioRequestDTO(
        @NotNull(message = "El estado es obligatorio") Boolean estado) {
}

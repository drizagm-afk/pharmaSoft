package com.edu.upeu.PharmaBackend.dto.auth;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record AsignarRolesRequestDTO(
        @NotEmpty(message = "Debe asignar al menos un rol")
        Set<@NotNull(message = "El id de rol no puede ser nulo") Long> rolIds) {
}

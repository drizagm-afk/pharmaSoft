package com.edu.upeu.PharmaBackend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VentaRequestDTO {
    @NotNull(message = "El cliente es obligatorio")
    @Positive(message = "El identificador del cliente debe ser válido")
    private Long clienteId;

    @NotEmpty(message = "La venta debe contener al menos un detalle")
    @Valid
    private List<DetalleVentaRequestDTO> detalles;
}
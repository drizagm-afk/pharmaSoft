package com.edu.upeu.PharmaBackend.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VentaResponseDTO {
    private Long id;
    private LocalDateTime fecha;

    private Long clienteId;
    private String clienteNombre;

    private String estado;
    private BigDecimal total;

    private List<DetalleVentaResponseDTO> detalles;
}
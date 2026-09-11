package com.edu.upeu.PharmaBackend.service.service;

import com.edu.upeu.PharmaBackend.dto.VentaRequestDTO;
import com.edu.upeu.PharmaBackend.dto.VentaResponseDTO;
import com.edu.upeu.PharmaBackend.enums.EstadoVenta;

import java.time.LocalDate;
import java.util.List;

public interface VentaService  {
    VentaResponseDTO create(VentaRequestDTO request);
    VentaResponseDTO read(Long id);
    List<VentaResponseDTO> readAll();
    List<VentaResponseDTO> buscar(
            Long clienteId,
            EstadoVenta estado,
            LocalDate desde,
            LocalDate hasta,
            String ordenarPor,
            String direccion
    );
}
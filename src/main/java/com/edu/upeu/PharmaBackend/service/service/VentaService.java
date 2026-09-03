package com.edu.upeu.PharmaBackend.service.service;

import com.edu.upeu.PharmaBackend.dto.VentaRequestDTO;
import com.edu.upeu.PharmaBackend.dto.VentaResponseDTO;

import java.util.List;

public interface VentaService  {
    VentaResponseDTO create(VentaRequestDTO request);
    VentaResponseDTO read(Long id);
    List<VentaResponseDTO> readAll();
}
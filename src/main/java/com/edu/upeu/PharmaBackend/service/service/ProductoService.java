package com.edu.upeu.PharmaBackend.service.service;

import com.edu.upeu.PharmaBackend.dto.PaginaResponseDTO;
import com.edu.upeu.PharmaBackend.dto.ProductoRequestDTO;
import com.edu.upeu.PharmaBackend.dto.ProductoResponseDTO;
import com.edu.upeu.PharmaBackend.service.generic.CrudService;

public interface ProductoService extends CrudService<ProductoRequestDTO, ProductoResponseDTO, Long> {
    PaginaResponseDTO<ProductoResponseDTO> listar(int pagina, int tamanio, String ordenarPor, String direccion);
}

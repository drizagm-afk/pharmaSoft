package com.edu.upeu.PharmaBackend.service.service;

import com.edu.upeu.PharmaBackend.dto.ProductoRequestDTO;
import com.edu.upeu.PharmaBackend.dto.ProductoResponseDTO;
import com.edu.upeu.PharmaBackend.service.generic.CrudService;
import com.edu.upeu.PharmaBackend.dto.PaginaResponse;

public interface ProductoService extends CrudService<ProductoRequestDTO, ProductoResponseDTO, Long> {
    PaginaResponse<ProductoResponseDTO> listar(int pagina, int tamanio, String ordenarPor, String direccion);
}

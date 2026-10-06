package com.edu.upeu.PharmaBackend.service.service;

import com.edu.upeu.PharmaBackend.dto.ClienteRequestDTO;
import com.edu.upeu.PharmaBackend.dto.ClienteResponseDTO;
import com.edu.upeu.PharmaBackend.service.generic.CrudService;

import java.util.List;
import com.edu.upeu.PharmaBackend.dto.PaginaResponseDTO;

public interface ClienteService extends CrudService<ClienteRequestDTO, ClienteResponseDTO, Long> {
    PaginaResponseDTO<ClienteResponseDTO> listar(int pagina, int tamanio, String ordenarPor, String direccion);

    List<ClienteResponseDTO> readPage(int page, int pageSize);
}
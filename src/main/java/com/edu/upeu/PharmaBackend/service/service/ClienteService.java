package com.edu.upeu.PharmaBackend.service.service;

import com.edu.upeu.PharmaBackend.dto.ClienteRequestDTO;
import com.edu.upeu.PharmaBackend.dto.ClienteResponseDTO;
import com.edu.upeu.PharmaBackend.service.generic.CrudService;
import com.edu.upeu.PharmaBackend.dto.PaginaResponse;

public interface ClienteService extends CrudService<ClienteRequestDTO, ClienteResponseDTO, Long> {
    PaginaResponse<ClienteResponseDTO> listar(int pagina, int tamanio, String ordenarPor, String direccion);
}

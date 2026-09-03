package com.edu.upeu.PharmaBackend.mapper;

import com.edu.upeu.PharmaBackend.dto.ClienteResponseDTO;
import com.edu.upeu.PharmaBackend.entity.Cliente;

public class ClienteMapper {
    static public ClienteResponseDTO ConvertToResponse(Cliente cliente) {
        return new ClienteResponseDTO(
                cliente.getId(),
                cliente.getDni(),
                cliente.getNombres(),
                cliente.getApellidos(),
                cliente.getEmail(),
                cliente.getTelefono(),
                cliente.getDireccion(),
                cliente.getEstado(),
                cliente.getFechaCreacion(),
                cliente.getFechaModificacion()
        );
    }
}
package com.edu.upeu.PharmaBackend.service.service;

import com.edu.upeu.PharmaBackend.dto.auth.EmpleadoResponseDTO;
import com.edu.upeu.PharmaBackend.dto.auth.RolResponseDTO;
import com.edu.upeu.PharmaBackend.dto.auth.UsuarioRequestDTO;
import com.edu.upeu.PharmaBackend.dto.auth.UsuarioResponseDTO;

import java.util.List;

public interface UsuarioService {

    UsuarioResponseDTO crear(UsuarioRequestDTO request);

    List<UsuarioResponseDTO> listar();

    List<EmpleadoResponseDTO> listarEmpleados();

    List<RolResponseDTO> listarRoles();
}

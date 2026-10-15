package com.edu.upeu.PharmaBackend.service.service;

import com.edu.upeu.PharmaBackend.dto.auth.LoginRequestDTO;
import com.edu.upeu.PharmaBackend.dto.auth.LoginResponseDTO;
import com.edu.upeu.PharmaBackend.dto.auth.UsuarioResponseDTO;

public interface AuthService {

    LoginResponseDTO login(LoginRequestDTO request);

    UsuarioResponseDTO usuarioActual(String username);
}

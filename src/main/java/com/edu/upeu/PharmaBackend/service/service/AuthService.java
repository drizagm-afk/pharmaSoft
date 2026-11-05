package com.edu.upeu.PharmaBackend.service.service;

import com.edu.upeu.PharmaBackend.dto.auth.CambioPasswordRequestDTO;

import com.edu.upeu.PharmaBackend.dto.auth.LoginRequestDTO;
import com.edu.upeu.PharmaBackend.dto.auth.LoginResponseDTO;
import com.edu.upeu.PharmaBackend.dto.auth.UsuarioResponseDTO;

public interface AuthService {
    void cambiarPassword(String username,
            CambioPasswordRequestDTO request);

    LoginResponseDTO login(LoginRequestDTO request);

    UsuarioResponseDTO usuarioActual(String username);
}

package com.edu.upeu.PharmaBackend.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import com.edu.upeu.PharmaBackend.dto.auth.CambioPasswordRequestDTO;
import com.edu.upeu.PharmaBackend.exception.ReglaNegocioException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import com.edu.upeu.PharmaBackend.auditoria.EventoAuditoria;
import com.edu.upeu.PharmaBackend.service.service.AuditoriaService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.edu.upeu.PharmaBackend.dto.auth.LoginRequestDTO;
import com.edu.upeu.PharmaBackend.dto.auth.LoginResponseDTO;
import com.edu.upeu.PharmaBackend.dto.auth.UsuarioResponseDTO;
import com.edu.upeu.PharmaBackend.entity.Usuario;
import com.edu.upeu.PharmaBackend.exception.RecursosNoEncontradosException;
import com.edu.upeu.PharmaBackend.mapper.UsuarioMapper;
import com.edu.upeu.PharmaBackend.repository.UsuarioRepository;
import com.edu.upeu.PharmaBackend.security.JwtService;
import com.edu.upeu.PharmaBackend.security.PermisosPorRol;
import com.edu.upeu.PharmaBackend.service.service.AuthService;

import java.util.List;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log =
            LoggerFactory.getLogger(AuthServiceImpl.class);

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;

    public AuthServiceImpl(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UsuarioRepository usuarioRepository,

            UsuarioMapper usuarioMapper,
            PasswordEncoder passwordEncoder,
            AuditoriaService auditoriaService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
        this.usuarioMapper = usuarioMapper;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaService = auditoriaService;
    }

    /*
     * 1. AuthenticationManager busca al usuario (UsuarioDetailsService),
     *    verifica que este habilitado y compara la contrasena con el hash
     *    usando BCrypt. Si algo falla lanza una AuthenticationException,
     *    que GlobalExceptionHandler convierte en 401.
     * 2. Si todo es correcto, se emite el JWT.
     *
     * La contrasena NUNCA se escribe en el log.
     */
    @Override
    @Transactional(readOnly = true)
    public LoginResponseDTO login(LoginRequestDTO request) {

        String username = request.getUsername().trim();

        log.info("Intento de inicio de sesión: usuario={}", username);

        Authentication autenticacion;
        try {
            autenticacion = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.getPassword()));
        } catch (AuthenticationException ex) {
            auditarLogin("LOGIN_FALLIDO", username);
            throw ex;
        }

        UserDetails principal = (UserDetails) autenticacion.getPrincipal();
        Usuario usuario = buscar(principal.getUsername());

        String token = jwtService.generarToken(principal);

        List<String> roles = usuarioMapper.nombresRoles(usuario.getRoles());
        auditarLogin("LOGIN_EXITOSO", usuario.getUsername());

        log.info("Inicio de sesión correcto: usuario={} roles={}",
                usuario.getUsername(), roles);

        return new LoginResponseDTO(
                token,
                "Bearer",
                jwtService.getExpiracionSegundos(),
                usuario.getUsername(),
                usuario.getEmpleado().getNombreCompleto(),
                roles,
                PermisosPorRol.permisos(usuario.getRoles())
        );
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponseDTO usuarioActual(String username) {
        return usuarioMapper.toUsuarioResponse(buscar(username));
    }

    @Override
    @Transactional
    public void cambiarPassword(String username,
            CambioPasswordRequestDTO request) {
        Usuario usuario = buscar(username);
        if (request.passwordActual().getBytes(StandardCharsets.UTF_8).length > 72
                || !passwordEncoder.matches(request.passwordActual(), usuario.getPassword())) {
            throw new ReglaNegocioException(
                    "La contraseña actual no es correcta");
        }
        if (request.passwordNueva().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ReglaNegocioException(
                    "La contraseña no puede superar 72 bytes");
        }
        if (passwordEncoder.matches(request.passwordNueva(), usuario.getPassword())) {
            throw new ReglaNegocioException(
                    "La nueva contraseña debe ser distinta de la actual");
        }
        usuario.setPassword(passwordEncoder.encode(request.passwordNueva()));
        usuarioRepository.save(usuario);
        log.info("Contraseña actualizada: usuario={}", usuario.getUsername());
    }

    private void auditarLogin(String accion, String username) {
        try {
            auditoriaService.registrar(EventoAuditoria.login(accion, username));
        } catch (RuntimeException ex) {
            log.error("No se pudo auditar {} | {}", accion, ex.getClass().getSimpleName());
        }
    }

    private Usuario buscar(String username) {
        return usuarioRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new RecursosNoEncontradosException(
                        "Usuario no encontrado: " + username));
    }
}

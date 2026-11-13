package com.edu.upeu.PharmaBackend.controller;

import com.edu.upeu.PharmaBackend.dto.AuditoriaEventoDTO;
import com.edu.upeu.PharmaBackend.dto.PaginaResponse;
import com.edu.upeu.PharmaBackend.service.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/auditoria")
@RequiredArgsConstructor
public class AuditoriaController {
    private final AuditoriaService auditoriaService;

    @GetMapping
    public PaginaResponse<AuditoriaEventoDTO> buscar(
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanio) {
        return auditoriaService.buscar(usuario, accion, desde, hasta, pagina, tamanio);
    }
}

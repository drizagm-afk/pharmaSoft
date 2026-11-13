package com.edu.upeu.PharmaBackend.service.service;

import com.edu.upeu.PharmaBackend.auditoria.EventoAuditoria;
import com.edu.upeu.PharmaBackend.dto.AuditoriaEventoDTO;
import com.edu.upeu.PharmaBackend.dto.PaginaResponse;
import java.time.LocalDate;

public interface AuditoriaService {
    void registrar(EventoAuditoria evento);
    PaginaResponse<AuditoriaEventoDTO> buscar(String usuario, String accion, LocalDate desde,
                                             LocalDate hasta, int pagina, int tamanio);
}

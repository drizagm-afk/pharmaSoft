package com.edu.upeu.PharmaBackend.dto;

import com.edu.upeu.PharmaBackend.entity.AuditoriaEvento;
import java.time.LocalDateTime;

public record AuditoriaEventoDTO(Long id, LocalDateTime fecha, String usuario, String accion,
                                 String entidad, Long entidadId, String detalle, String requestId) {
    public static AuditoriaEventoDTO de(AuditoriaEvento evento) {
        return new AuditoriaEventoDTO(evento.getId(), evento.getFecha(), evento.getUsuario(),
                evento.getAccion(), evento.getEntidad(), evento.getEntidadId(),
                evento.getDetalle(), evento.getRequestId());
    }
}

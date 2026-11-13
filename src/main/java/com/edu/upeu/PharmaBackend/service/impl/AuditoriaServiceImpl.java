package com.edu.upeu.PharmaBackend.service.impl;

import com.edu.upeu.PharmaBackend.auditoria.EventoAuditoria;
import com.edu.upeu.PharmaBackend.dto.AuditoriaEventoDTO;
import com.edu.upeu.PharmaBackend.dto.PaginaResponse;
import com.edu.upeu.PharmaBackend.entity.AuditoriaEvento;
import com.edu.upeu.PharmaBackend.repository.AuditoriaEventoRepository;
import com.edu.upeu.PharmaBackend.service.service.AuditoriaService;
import com.edu.upeu.PharmaBackend.service.support.Paginacion;
import com.edu.upeu.PharmaBackend.service.support.RangoFechas;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuditoriaServiceImpl implements AuditoriaService {
    private static final Logger log = LoggerFactory.getLogger(AuditoriaServiceImpl.class);
    private final AuditoriaEventoRepository repository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(EventoAuditoria evento) {
        AuditoriaEvento registro = new AuditoriaEvento();
        registro.setFecha(LocalDateTime.now());
        registro.setUsuario(evento.usuario());
        registro.setAccion(evento.accion());
        registro.setEntidad(evento.entidad());
        registro.setEntidadId(evento.entidadId());
        registro.setDetalle(evento.detalle());
        registro.setRequestId(evento.requestId());
        repository.save(registro);
        log.info("Auditoría {} | {} {} | usuario={}", evento.accion(),
                evento.entidad(), evento.entidadId(), evento.usuario());
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<AuditoriaEventoDTO> buscar(String usuario, String accion,
            LocalDate desde, LocalDate hasta, int pagina, int tamanio) {
        var pageable = Paginacion.crear(pagina, tamanio, "fecha", "desc",
                "fecha", "desc", Set.of("fecha", "usuario", "accion"));
        var rango = RangoFechas.de(desde, hasta);
        return PaginaResponse.de(repository.buscar(vacioANulo(usuario), vacioANulo(accion),
                rango.desde(), hasta == null ? null : hasta.plusDays(1).atStartOfDay(),
                pageable).map(AuditoriaEventoDTO::de));
    }

    private static String vacioANulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}

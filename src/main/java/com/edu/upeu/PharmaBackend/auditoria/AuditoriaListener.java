package com.edu.upeu.PharmaBackend.auditoria;

import com.edu.upeu.PharmaBackend.service.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class AuditoriaListener {
    private static final Logger log = LoggerFactory.getLogger(AuditoriaListener.class);
    private final AuditoriaService auditoriaService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alConfirmar(EventoAuditoria evento) {
        try {
            auditoriaService.registrar(evento);
        } catch (RuntimeException ex) {
            log.error("No se pudo auditar {} {} | {}", evento.accion(),
                    evento.entidadId(), ex.getClass().getSimpleName());
        }
    }
}

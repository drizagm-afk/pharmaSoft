package com.edu.upeu.PharmaBackend.auditoria;

import com.edu.upeu.PharmaBackend.service.service.AuditoriaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class EventoAuditoriaTest {
    @AfterEach
    void limpiar() { MDC.clear(); SecurityContextHolder.clearContext(); }

    @Test
    void capturaContextoAntesDelCommitYLimitaElDetalle() {
        MDC.put("requestId", "a12bc345");
        var evento = EventoAuditoria.de("PRODUCTO_ELIMINADO", "Producto", 1L, "a".repeat(600));
        MDC.clear();
        assertThat(evento.requestId()).isEqualTo("a12bc345");
        assertThat(evento.detalle()).hasSize(500);
        assertThat(evento.usuario()).isEqualTo("anonimo");
        var login = EventoAuditoria.login("LOGIN_FALLIDO", "a".repeat(80));
        assertThat(login.usuario()).hasSize(50);
        assertThat(login.detalle()).isNull();
    }

    @Test
    void falloAlAuditarNoPropagaErrorDespuesDelCommit() {
        AuditoriaService servicio = mock(AuditoriaService.class);
        var evento = EventoAuditoria.login("LOGIN_EXITOSO", "rosa.mendoza");
        doThrow(new IllegalStateException("almacenamiento no disponible")).when(servicio).registrar(evento);
        assertThatCode(() -> new AuditoriaListener(servicio).alConfirmar(evento)).doesNotThrowAnyException();
        verify(servicio).registrar(evento);
    }
}

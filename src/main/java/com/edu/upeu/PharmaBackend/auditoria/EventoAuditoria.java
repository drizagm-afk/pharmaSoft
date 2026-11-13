package com.edu.upeu.PharmaBackend.auditoria;

import org.slf4j.MDC;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.edu.upeu.PharmaBackend.config.RequestIdFilter;

public record EventoAuditoria(String usuario, String accion, String entidad,
                              Long entidadId, String detalle, String requestId) {
    public static EventoAuditoria de(String accion, String entidad, Long entidadId, String detalle) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String usuario = auth != null && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken) ? auth.getName() : "anonimo";
        return new EventoAuditoria(recortar(usuario, 50), accion, entidad, entidadId,
                recortar(detalle, 500), MDC.get(RequestIdFilter.CLAVE_MDC));
    }

    // Login has no authenticated security context yet. Never include credentials or the JWT.
    public static EventoAuditoria login(String accion, String username) {
        return new EventoAuditoria(recortar(username, 50), accion, "Usuario",
                null, null, MDC.get(RequestIdFilter.CLAVE_MDC));
    }

    private static String recortar(String texto, int maximo) {
        return texto == null || texto.length() <= maximo ? texto : texto.substring(0, maximo);
    }
}

package com.edu.upeu.PharmaBackend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import com.edu.upeu.PharmaBackend.config.RequestIdFilter;
import org.slf4j.MDC;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

final class RespuestaErrorSeguridad {

    private RespuestaErrorSeguridad() {}

    static void escribir(
            HttpServletRequest request,
            HttpServletResponse response,
            int status,
            String error,
            String mensaje) throws IOException {

        String requestId = MDC.get(RequestIdFilter.CLAVE_MDC);
        String json = "{"
                + "\"timestamp\":\"" + LocalDateTime.now() + "\","
                + "\"status\":" + status + ","
                + "\"error\":\"" + escapar(error) + "\","
                + "\"message\":\"" + escapar(mensaje) + "\","
                + "\"path\":\"" + escapar(request.getRequestURI()) + "\","
                + "\"validationErrors\":null,"
                + "\"requestId\":" + (requestId == null ? "null" : "\"" + escapar(requestId) + "\"")
                + "}";

        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(json);
    }

    private static String escapar(String texto) {
        if (texto == null) return "";

        return texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

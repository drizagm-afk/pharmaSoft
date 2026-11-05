package com.edu.upeu.PharmaBackend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {
    public static final String CABECERA = "X-Request-Id";
    public static final String CLAVE_MDC = "requestId";
    private static final Logger log = LoggerFactory.getLogger(RequestIdFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                   FilterChain chain) throws ServletException, IOException {
        // Generate our own ID; do not copy untrusted header text into logs.
        String id = UUID.randomUUID().toString().substring(0, 8);
        MDC.put(CLAVE_MDC, id);
        response.setHeader(CABECERA, id);
        long inicio = System.nanoTime();
        boolean fallida = true;
        try {
            chain.doFilter(request, response);
            fallida = false;
        } finally {
            try {
                String ruta = request.getRequestURI();
                if (!ruta.equals("/actuator") && !ruta.startsWith("/actuator/")) {
                    // Never log headers, query strings, request bodies or response bodies.
                    log.info("{} {} -> {} ({} ms)", request.getMethod(), ruta,
                            fallida ? 500 : response.getStatus(),
                            TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio));
                }
            } finally {
                MDC.remove(CLAVE_MDC);
            }
        }
    }
}

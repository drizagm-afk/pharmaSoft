package com.edu.upeu.PharmaBackend.service.support;

import com.edu.upeu.PharmaBackend.exception.ReglaNegocioException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record RangoFechas(LocalDateTime desde, LocalDateTime hasta) {
    public static RangoFechas de(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new ReglaNegocioException("El rango de fechas es inválido: 'desde' ("
                    + desde + ") es posterior a 'hasta' (" + hasta + ")");
        }
        return new RangoFechas(desde == null ? null : desde.atStartOfDay(),
                hasta == null ? null : hasta.atTime(LocalTime.MAX));
    }
}

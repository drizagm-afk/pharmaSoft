package com.edu.upeu.PharmaBackend.service.support;

import com.edu.upeu.PharmaBackend.exception.ReglaNegocioException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class Paginacion {
    private Paginacion() {}

    public static PageRequest crear(int pagina, int tamanio, String ordenarPor, String direccion,
                                    String campoDefecto, String direccionDefecto, Set<String> permitidos) {
        if (pagina < 0) {
            throw new ReglaNegocioException("La página debe ser mayor o igual a cero");
        }
        if (tamanio < 1 || tamanio > 100) {
            throw new ReglaNegocioException("El tamaño de página debe estar entre 1 y 100");
        }
        String campo = ordenarPor == null || ordenarPor.isBlank() ? campoDefecto : ordenarPor.trim();
        if (!permitidos.contains(campo)) {
            throw new ReglaNegocioException("El campo de ordenamiento '" + campo
                    + "' no está permitido. Campos válidos: " + permitidos);
        }
        String sentido = direccion == null || direccion.isBlank() ? direccionDefecto : direccion.trim();
        if (!sentido.equalsIgnoreCase("asc") && !sentido.equalsIgnoreCase("desc")) {
            throw new ReglaNegocioException("La dirección de ordenamiento debe ser asc o desc");
        }
        Sort sort = Sort.by(Sort.Direction.fromString(sentido), campo);
        if (!campo.equals("id")) sort = sort.and(Sort.by("id"));
        return PageRequest.of(pagina, tamanio, sort);
    }
}

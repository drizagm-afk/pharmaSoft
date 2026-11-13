package com.edu.upeu.PharmaBackend.service.support;

import java.text.Normalizer;
import java.util.Locale;

public final class Busqueda {
    private Busqueda() {}

    public static String normalizar(String texto) {
        if (texto == null) return "";
        return Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toUpperCase(Locale.ROOT);
    }
}

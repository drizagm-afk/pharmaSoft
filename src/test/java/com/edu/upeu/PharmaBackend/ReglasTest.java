package com.edu.upeu.PharmaBackend;

import com.edu.upeu.PharmaBackend.dto.*;
import com.edu.upeu.PharmaBackend.exception.ReglaNegocioException;
import com.edu.upeu.PharmaBackend.service.support.*;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Set;
import static org.assertj.core.api.Assertions.*;

class ReglasTest {
    @Test
    void paginacionValidaYOrdenEstable() {
        var pagina = Paginacion.crear(2, 10, "total", "DESC", "fecha", "desc", Set.of("total", "fecha"));
        assertThat(pagina.getPageNumber()).isEqualTo(2);
        assertThat(pagina.getSort().getOrderFor("total").isDescending()).isTrue();
        assertThat(pagina.getSort().getOrderFor("id")).isNotNull();
    }

    @Test
    void rechazaPaginaTamanioOrdenYDireccionInvalidos() {
        for (int[] valores : new int[][] {{-1, 10}, {0, 0}, {0, 101}}) {
            assertThatThrownBy(() -> Paginacion.crear(valores[0], valores[1], null, null, "id", "asc", Set.of("id")))
                    .isInstanceOf(ReglaNegocioException.class);
        }
        assertThatThrownBy(() -> Paginacion.crear(0, 10, "cliente.email", "asc", "id", "asc", Set.of("id")))
                .isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> Paginacion.crear(0, 10, "id", "invalid", "id", "asc", Set.of("id")))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void rangoIncluyeElDiaCompletoYPermiteLimitesOpcionales() {
        var dia = LocalDate.of(2026, 10, 11);
        var rango = RangoFechas.de(dia, dia);
        assertThat(rango.desde()).isEqualTo(dia.atStartOfDay());
        assertThat(rango.hasta()).isEqualTo(dia.atTime(LocalTime.MAX));
        assertThat(RangoFechas.de(null, null).desde()).isNull();
        assertThatThrownBy(() -> RangoFechas.de(dia.plusDays(1), dia)).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void validaDetalleNuloYCantidadNoPositiva() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var request = new VentaRequestDTO(1L, Arrays.asList(null, new DetalleVentaRequestDTO(1L, 0)));
            assertThat(validator.validate(request)).hasSize(2);
        }
    }
}

package com.edu.upeu.PharmaBackend.service.impl;

import com.edu.upeu.PharmaBackend.dto.reporte.ProductoMasVendidoDTO;
import com.edu.upeu.PharmaBackend.dto.reporte.VentaPorCategoriaDTO;
import com.edu.upeu.PharmaBackend.repository.VentaRepository;
import com.edu.upeu.PharmaBackend.service.service.ReporteService;
import com.edu.upeu.PharmaBackend.service.support.RangoFechas;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReporteServiceImpl implements ReporteService {
    private final VentaRepository ventaRepository;

    @Override
    public List<VentaPorCategoriaDTO> ventasPorCategoria(LocalDate desde, LocalDate hasta) {
        var rango = RangoFechas.de(desde, hasta);
        long inicio = System.currentTimeMillis();
        var resultado = ventaRepository.reporteVentasPorCategoria(rango.desde(), rango.hasta());
        log.info("Reporte ventas por categoria | desde={} | hasta={} | filas={} | duracionMs={}",
                desde, hasta, resultado.size(), System.currentTimeMillis() - inicio);
        return resultado;
    }

    @Override
    public List<ProductoMasVendidoDTO> productosMasVendidos(LocalDate desde, LocalDate hasta) {
        var rango = RangoFechas.de(desde, hasta);
        long inicio = System.currentTimeMillis();
        var resultado = ventaRepository.reporteProductosMasVendidos(rango.desde(), rango.hasta());
        log.info("Reporte productos mas vendidos | desde={} | hasta={} | filas={} | duracionMs={}",
                desde, hasta, resultado.size(), System.currentTimeMillis() - inicio);
        return resultado;
    }
}

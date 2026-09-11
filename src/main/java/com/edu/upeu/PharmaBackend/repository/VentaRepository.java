package com.edu.upeu.PharmaBackend.repository;

import com.edu.upeu.PharmaBackend.dto.reporte.ProductoMasVendidoDTO;
import com.edu.upeu.PharmaBackend.dto.reporte.VentaPorCategoriaDTO;
import com.edu.upeu.PharmaBackend.enums.EstadoVenta;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import com.edu.upeu.PharmaBackend.entity.Venta;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface VentaRepository extends JpaRepository<Venta, Long> {
    @Query("""
SELECT DISTINCT v FROM Venta v
    LEFT JOIN FETCH v.cliente c
    LEFT JOIN FETCH v.detalles d
    LEFT JOIN FETCH d.producto p
WHERE (:clienteId IS NULL OR c.id = :clienteId)
    AND (:estado IS NULL OR v.estado = :estado)
    AND (:desde IS NULL OR v.fecha >= :desde)
    AND (:hasta IS NULL OR v.fecha <= :hasta)"""
    )
    List<Venta> buscar(
            @Param("clienteId") Long clienteId,
            @Param("estado") EstadoVenta estado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            Sort sort
    );

    @Query("""
            select new com.edu.upeu.PharmaBackend.dto.reporte.VentaPorCategoriaDTO(
                       cat.id,
                       cat.nombre,
                       sum(d.cantidad),
                       sum(d.subtotal))
            from DetalleVenta d
            join d.venta v
            join d.producto p
            join p.categoria cat
            where v.estado = com.edu.upeu.PharmaBackend.enums.EstadoVenta.REGISTRADA
              and (:desde is null or v.fecha >= :desde)
              and (:hasta is null or v.fecha <= :hasta)
            group by cat.id, cat.nombre
            order by sum(d.subtotal) desc
            """)
    List<VentaPorCategoriaDTO> reporteVentasPorCategoria(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Query("""
            select new com.edu.upeu.PharmaBackend.dto.reporte.ProductoMasVendidoDTO(
                       p.id,
                       p.nombre,
                       cat.nombre,
                       sum(d.cantidad),
                       sum(d.subtotal))
            from DetalleVenta d
            join d.venta v
            join d.producto p
            join p.categoria cat
            where v.estado = com.edu.upeu.PharmaBackend.enums.EstadoVenta.REGISTRADA
              and (:desde is null or v.fecha >= :desde)
              and (:hasta is null or v.fecha <= :hasta)
            group by p.id, p.nombre, cat.nombre
            order by sum(d.cantidad) desc
            """)
    List<ProductoMasVendidoDTO> reporteProductosMasVendidos(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );
}
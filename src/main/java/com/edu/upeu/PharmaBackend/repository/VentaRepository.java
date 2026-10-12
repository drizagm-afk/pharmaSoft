package com.edu.upeu.PharmaBackend.repository;

import com.edu.upeu.PharmaBackend.dto.reporte.ProductoMasVendidoDTO;
import com.edu.upeu.PharmaBackend.dto.reporte.VentaPorCategoriaDTO;
import com.edu.upeu.PharmaBackend.enums.EstadoVenta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.edu.upeu.PharmaBackend.entity.Venta;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface VentaRepository extends JpaRepository<Venta, Long> {
    @Query(value = """
            SELECT v FROM Venta v
            WHERE (:clienteId IS NULL OR v.cliente.id = :clienteId)
                AND (:estado IS NULL OR v.estado = :estado)
                AND (:desde IS NULL OR v.fecha >= :desde)
                AND (:hasta IS NULL OR v.fecha <= :hasta)
            """, countQuery = """
            SELECT COUNT(v) FROM Venta v
            WHERE (:clienteId IS NULL OR v.cliente.id = :clienteId)
                AND (:estado IS NULL OR v.estado = :estado)
                AND (:desde IS NULL OR v.fecha >= :desde)
                AND (:hasta IS NULL OR v.fecha <= :hasta)
            """)
    Page<Venta> buscar(
            @Param("clienteId") Long clienteId,
            @Param("estado") EstadoVenta estado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            Pageable pageable
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

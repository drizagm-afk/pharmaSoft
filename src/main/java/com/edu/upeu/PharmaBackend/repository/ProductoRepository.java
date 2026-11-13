package com.edu.upeu.PharmaBackend.repository;

import com.edu.upeu.PharmaBackend.entity.Producto;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.time.LocalDateTime;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    @Query("""
            select p from Producto p
            where p.estado = true and p.stock > 0
              and (locate(:texto, translate(upper(p.nombre), 'ÁÉÍÓÚÜÑ', 'AEIOUUN')) > 0
                or locate(:texto, translate(upper(p.categoria.nombre), 'ÁÉÍÓÚÜÑ', 'AEIOUUN')) > 0)
            """)
    Page<Producto> buscarParaVenta(@Param("texto") String texto, Pageable pageable);

    boolean existsByNombreIgnoreCase(@NotNull String nombre);
    boolean existsByNombreIgnoreCaseAndIdNot(@NotNull String nombre, @NotNull Long id);

    List<Producto> findByCategoriaId(Long categoriaId);
    boolean existsByCategoriaId(Long categoriaId);

    /** The database checks availability and deducts stock in a single locked update. */
    @Modifying(flushAutomatically = true)
    @Query("""
            UPDATE Producto p
            SET p.stock = p.stock - :cantidad, p.fechaModificacion = :fecha
            WHERE p.id = :id AND p.estado = true AND p.stock >= :cantidad
            """)
    int descontarStock(@Param("id") Long id,
                       @Param("cantidad") int cantidad,
                       @Param("fecha") LocalDateTime fecha);
}

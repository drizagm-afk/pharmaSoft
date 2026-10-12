package com.edu.upeu.PharmaBackend.repository;

import com.edu.upeu.PharmaBackend.entity.Producto;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.time.LocalDateTime;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
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

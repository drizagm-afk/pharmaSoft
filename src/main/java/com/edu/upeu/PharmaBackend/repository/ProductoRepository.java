package com.edu.upeu.PharmaBackend.repository;

import com.edu.upeu.PharmaBackend.entity.Producto;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    boolean existsByNombreIgnoreCase(@NotNull String nombre);
    boolean existsByNombreIgnoreCaseAndIdNot(@NotNull String nombre, @NotNull Long id);

    List<Producto> findByCategoriaId(Long categoriaId);
    boolean existsByCategoriaId(Long categoriaId);
}
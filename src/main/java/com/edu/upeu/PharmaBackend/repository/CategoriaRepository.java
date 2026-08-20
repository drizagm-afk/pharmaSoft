package com.edu.upeu.PharmaBackend.repository;

import com.edu.upeu.PharmaBackend.entity.Categoria;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    boolean existsByNombreIgnoreCase(@NotNull String nombre);
    boolean existsByNombreIgnoreCaseAndIdNot(@NotNull String nombre, @NotNull Long id);
}

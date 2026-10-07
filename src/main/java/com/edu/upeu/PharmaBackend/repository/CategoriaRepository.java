package com.edu.upeu.PharmaBackend.repository;

import com.edu.upeu.PharmaBackend.entity.Categoria;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select c from Categoria c where c.id = :id")
    java.util.Optional<Categoria> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);

    boolean existsByNombreIgnoreCase(@NotNull String nombre);
    boolean existsByNombreIgnoreCaseAndIdNot(@NotNull String nombre, @NotNull Long id);
}

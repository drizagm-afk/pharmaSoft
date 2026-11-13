package com.edu.upeu.PharmaBackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.edu.upeu.PharmaBackend.entity.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    @Query("""
            select c from Cliente c
            where c.estado = true
              and (locate(:texto, translate(upper(concat(c.nombres, ' ', c.apellidos)),
                       'ÁÉÍÓÚÜÑ', 'AEIOUUN')) > 0
                or locate(:texto, translate(upper(concat(c.apellidos, ' ', c.nombres)),
                       'ÁÉÍÓÚÜÑ', 'AEIOUUN')) > 0
                or locate(:texto, c.dni) > 0)
            """)
    Page<Cliente> buscarParaVenta(@Param("texto") String texto, Pageable pageable);


    boolean existsByDni(String dni);
    boolean existsByEmailIgnoreCase(String email);

    boolean existsByDniAndIdNot(String dni, Long id);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
}

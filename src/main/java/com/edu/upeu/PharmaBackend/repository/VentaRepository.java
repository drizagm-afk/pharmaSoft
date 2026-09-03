package com.edu.upeu.PharmaBackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.edu.upeu.PharmaBackend.entity.Venta;

public interface VentaRepository extends JpaRepository<Venta, Long> {
}
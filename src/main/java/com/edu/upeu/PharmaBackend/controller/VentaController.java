package com.edu.upeu.PharmaBackend.controller;

import com.edu.upeu.PharmaBackend.dto.VentaRequestDTO;
import com.edu.upeu.PharmaBackend.dto.VentaResponseDTO;
import com.edu.upeu.PharmaBackend.service.service.VentaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/ventas")
public class VentaController {
    private final VentaService ventaService;

    //READ
    @GetMapping
    public ResponseEntity<Iterable<VentaResponseDTO>> getVentas() {
        return ResponseEntity.ok(
                ventaService.readAll()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<VentaResponseDTO> getVenta(@PathVariable Long id) {
        return ResponseEntity.ok(
                ventaService.read(id)
        );
    }

    //CREATE
    @PostMapping
    public ResponseEntity<VentaResponseDTO> create(
            @Valid @RequestBody VentaRequestDTO request
    ) {
        VentaResponseDTO ventaResponseDTO = ventaService.create(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(ventaResponseDTO);
    }
}
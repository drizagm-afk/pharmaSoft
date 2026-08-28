package com.edu.upeu.PharmaBackend.controller;

import com.edu.upeu.PharmaBackend.dto.ProductoRequestDTO;
import com.edu.upeu.PharmaBackend.dto.ProductoResponseDTO;
import com.edu.upeu.PharmaBackend.service.service.ProductoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/productos")
public class ProductoController {
    private final ProductoService productoService;

    //READ
    @GetMapping
    public ResponseEntity<Iterable<ProductoResponseDTO>> getProductos() {
        return ResponseEntity.ok(
                productoService.readAll()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponseDTO> getProducto(@PathVariable Long id) {
        return ResponseEntity.ok(
                productoService.read(id)
        );
    }

    //CREATE
    @PostMapping
    public ResponseEntity<ProductoResponseDTO> create(
            @Valid @RequestBody ProductoRequestDTO request
    ) {
        ProductoResponseDTO productoResponseDTO = productoService.create(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(productoResponseDTO);
    }

    //UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<ProductoResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductoRequestDTO request
    ) {
        ProductoResponseDTO productoResponseDTO = productoService.update(id, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(productoResponseDTO);
    }

    //DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<ProductoResponseDTO> delete(@PathVariable Long id) {
        productoService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
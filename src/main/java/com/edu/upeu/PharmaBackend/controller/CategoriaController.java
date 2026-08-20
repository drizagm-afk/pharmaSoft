package com.edu.upeu.PharmaBackend.controller;

import com.edu.upeu.PharmaBackend.dto.CategoriaRequestDTO;
import com.edu.upeu.PharmaBackend.dto.CategoriaResponseDTO;
import com.edu.upeu.PharmaBackend.service.service.CategoriaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/categorias")
public class CategoriaController {
    private final CategoriaService categoriaService;

    //READ
    @GetMapping
    public ResponseEntity<Iterable<CategoriaResponseDTO>> getCategorias() {
        return ResponseEntity.ok(
                categoriaService.readAll()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoriaResponseDTO> getCategoria(@PathVariable Long id) {
        return ResponseEntity.ok(
                categoriaService.read(id)
        );
    }

    //CREATE
    @PostMapping
    public ResponseEntity<CategoriaResponseDTO> create(
            @Valid @RequestBody CategoriaRequestDTO request
    ) {
        CategoriaResponseDTO categoriaResponseDTO = categoriaService.create(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(categoriaResponseDTO);
    }

    //UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<CategoriaResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody CategoriaRequestDTO request
    ) {
        CategoriaResponseDTO categoriaResponseDTO = categoriaService.update(id, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(categoriaResponseDTO);
    }

    //DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<CategoriaResponseDTO> delete(@PathVariable Long id) {
        categoriaService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
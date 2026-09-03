package com.edu.upeu.PharmaBackend.controller;

import com.edu.upeu.PharmaBackend.dto.ClienteRequestDTO;
import com.edu.upeu.PharmaBackend.dto.ClienteResponseDTO;
import com.edu.upeu.PharmaBackend.service.service.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/clientes")
public class ClienteController {
    private final ClienteService clienteService;

    //READ
    @GetMapping
    public ResponseEntity<Iterable<ClienteResponseDTO>> getClientes() {
        return ResponseEntity.ok(
                clienteService.readAll()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> getCliente(@PathVariable Long id) {
        return ResponseEntity.ok(
                clienteService.read(id)
        );
    }

    //CREATE
    @PostMapping
    public ResponseEntity<ClienteResponseDTO> create(
            @Valid @RequestBody ClienteRequestDTO request
    ) {
        ClienteResponseDTO clienteResponseDTO = clienteService.create(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(clienteResponseDTO);
    }

    //UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody ClienteRequestDTO request
    ) {
        ClienteResponseDTO clienteResponseDTO = clienteService.update(id, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(clienteResponseDTO);
    }

    //DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> delete(@PathVariable Long id) {
        clienteService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
package com.edu.upeu.PharmaBackend.controller;

import com.edu.upeu.PharmaBackend.dto.ClienteRequestDTO;
import com.edu.upeu.PharmaBackend.dto.ClienteResponseDTO;
import com.edu.upeu.PharmaBackend.service.service.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.edu.upeu.PharmaBackend.dto.PaginaResponseDTO;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/clientes")
public class ClienteController {
    private final ClienteService clienteService;

    //READ
    @GetMapping
    public ResponseEntity<PaginaResponseDTO<ClienteResponseDTO>> getClientes(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanio,
            @RequestParam(defaultValue = "apellidos") String ordenarPor,
            @RequestParam(defaultValue = "asc") String direccion) {
        return ResponseEntity.ok(clienteService.listar(pagina, tamanio, ordenarPor, direccion));
    }

    @GetMapping("/page/{page}/{pageSize}")
    public ResponseEntity<List<ClienteResponseDTO>> getClientesPage(
            @PathVariable int page, @PathVariable int pageSize
    ) {
        return ResponseEntity.ok(
                clienteService.readPage(page, pageSize)
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

        return ResponseEntity.ok(clienteResponseDTO);
    }

    //DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        clienteService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
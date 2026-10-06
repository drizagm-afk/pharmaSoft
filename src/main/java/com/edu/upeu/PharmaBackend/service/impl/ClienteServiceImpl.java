package com.edu.upeu.PharmaBackend.service.impl;

import com.edu.upeu.PharmaBackend.mapper.ClienteMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.edu.upeu.PharmaBackend.dto.ClienteRequestDTO;
import com.edu.upeu.PharmaBackend.dto.ClienteResponseDTO;
import com.edu.upeu.PharmaBackend.entity.Cliente;
import com.edu.upeu.PharmaBackend.exception.RecursosNoEncontradosException;
import com.edu.upeu.PharmaBackend.exception.ReglaNegocioException;
import com.edu.upeu.PharmaBackend.repository.ClienteRepository;
import com.edu.upeu.PharmaBackend.service.service.ClienteService;

import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import com.edu.upeu.PharmaBackend.dto.PaginaResponseDTO;

@Service
public class ClienteServiceImpl implements ClienteService {

    private static final Logger log =
            LoggerFactory.getLogger(ClienteServiceImpl.class);

    private final ClienteRepository clienteRepository;

    public ClienteServiceImpl(
            ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Override
    @Transactional
    public ClienteResponseDTO create(ClienteRequestDTO request) {
        log.info(
                "Registrando cliente con DNI={}",
                request.getDni()
        );

        String dni = request.getDni().trim();
        String email = request.getEmail()
                .trim()
                .toLowerCase();

        // Regla de negocio 1
        if (clienteRepository.existsByDni(dni)) {
            throw new ReglaNegocioException(
                    "Ya existe un cliente con el DNI: " + dni
            );
        }

        // Regla de negocio 2
        if (clienteRepository.existsByEmailIgnoreCase(email)) {
            throw new ReglaNegocioException(
                    "Ya existe un cliente con el correo: " + email
            );
        }

        Cliente cliente = new Cliente();

        cliente.setDni(dni);
        cliente.setNombres(
                request.getNombres().trim()
        );
        cliente.setApellidos(
                request.getApellidos().trim()
        );
        cliente.setEmail(email);
        cliente.setTelefono(
                normalizar(request.getTelefono())
        );
        cliente.setDireccion(
                normalizar(request.getDireccion())
        );
        cliente.setEstado(request.getEstado());

        Cliente guardado =
                clienteRepository.save(cliente);

        log.info(
                "Cliente registrado correctamente id={}",
                guardado.getId()
        );

        return ClienteMapper.ConvertToResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponseDTO read(Long id) {

        log.info("Buscando cliente id={}", id);

        Cliente cliente =
                clienteRepository.findById(id)
                        .orElseThrow(() ->
                                new RecursosNoEncontradosException(
                                        "Cliente no encontrado con id: " + id
                                )
                        );

        return ClienteMapper.ConvertToResponse(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> readPage(int page, int pageSize) {
        log.info("Listando pagina de clientes");

        return clienteRepository.findAll(PageRequest.of(page, pageSize))
                .stream()
                .map(ClienteMapper::ConvertToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> readAll() {
        log.info("Listando clientes");

        return clienteRepository.findAll()
                .stream()
                .map(ClienteMapper::ConvertToResponse)
                .toList();
    }

    @Override
    @Transactional
    public ClienteResponseDTO update(
            Long id,
            ClienteRequestDTO request) {

        Cliente cliente =
                clienteRepository.findById(id)
                        .orElseThrow(() ->
                                new RecursosNoEncontradosException(
                                        "Cliente no encontrado con id: " + id
                                )
                        );

        String dni = request.getDni().trim();
        String email = request.getEmail()
                .trim()
                .toLowerCase();

        // DNI de otro cliente
        if (clienteRepository
                .existsByDniAndIdNot(dni, id)) {

            throw new ReglaNegocioException(
                    "Ya existe otro cliente con el DNI: "
                            + dni
            );
        }

        // Email de otro cliente
        if (clienteRepository
                .existsByEmailIgnoreCaseAndIdNot(
                        email,
                        id)) {

            throw new ReglaNegocioException(
                    "Ya existe otro cliente con el correo: "
                            + email
            );
        }

        cliente.setDni(dni);
        cliente.setNombres(
                request.getNombres().trim()
        );
        cliente.setApellidos(
                request.getApellidos().trim()
        );
        cliente.setEmail(email);
        cliente.setTelefono(
                normalizar(request.getTelefono())
        );
        cliente.setDireccion(
                normalizar(request.getDireccion())
        );
        cliente.setEstado(request.getEstado());

        Cliente actualizado =
                clienteRepository.save(cliente);

        log.info(
                "Cliente id={} actualizado correctamente",
                id
        );

        return ClienteMapper.ConvertToResponse(actualizado);
    }

    @Override
    @Transactional
    public void delete(Long id) {

        Cliente cliente =
                clienteRepository.findById(id)
                        .orElseThrow(() ->
                                new RecursosNoEncontradosException(
                                        "Cliente no encontrado con id: " + id
                                )
                        );

        if (!Boolean.TRUE.equals(cliente.getEstado())) {
            throw new ReglaNegocioException("El cliente ya está inactivo.");
        }
        cliente.setEstado(false);
        clienteRepository.save(cliente);

        log.info(
                "Cliente id={} dado de baja correctamente",
                id
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponseDTO<ClienteResponseDTO> listar(
            int pagina, int tamanio, String ordenarPor, String direccion) {
        if (pagina < 0 || tamanio < 1 || tamanio > 100) {
            throw new ReglaNegocioException("La página debe ser mayor o igual a 0 y el tamaño debe estar entre 1 y 100.");
        }
        if (ordenarPor == null || !Set.of("id", "dni", "nombres", "apellidos", "email").contains(ordenarPor)) {
            throw new ReglaNegocioException("El campo de orden no está permitido.");
        }
        if (!"asc".equalsIgnoreCase(direccion) && !"desc".equalsIgnoreCase(direccion)) {
            throw new ReglaNegocioException("La dirección debe ser asc o desc.");
        }
        Sort orden = Sort.by(Sort.Direction.fromString(direccion), ordenarPor);
        if (!"id".equals(ordenarPor)) {
            orden = orden.and(Sort.by(Sort.Direction.ASC, "id"));
        }
        Page<ClienteResponseDTO> resultado = clienteRepository
                .findAll(PageRequest.of(pagina, tamanio, orden))
                .map(ClienteMapper::ConvertToResponse);
        return new PaginaResponseDTO<>(resultado.getContent(), resultado.getNumber(),
                resultado.getSize(), resultado.getTotalElements(), resultado.getTotalPages(), resultado.isLast());
    }

    private String normalizar(String valor) {

        if (valor == null || valor.isBlank()) {
            return null;
        }

        return valor.trim();
    }
}
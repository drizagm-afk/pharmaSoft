package com.edu.upeu.PharmaBackend.service.impl;

import com.edu.upeu.PharmaBackend.dto.ProductoRequestDTO;
import com.edu.upeu.PharmaBackend.dto.ProductoResponseDTO;
import com.edu.upeu.PharmaBackend.entity.Categoria;
import com.edu.upeu.PharmaBackend.entity.Producto;
import com.edu.upeu.PharmaBackend.exception.RecursosNoEncontradosException;
import com.edu.upeu.PharmaBackend.exception.ReglaNegocioException;
import com.edu.upeu.PharmaBackend.mapper.ProductoMapper;
import com.edu.upeu.PharmaBackend.repository.CategoriaRepository;
import com.edu.upeu.PharmaBackend.repository.ProductoRepository;
import com.edu.upeu.PharmaBackend.service.service.ProductoService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.edu.upeu.PharmaBackend.dto.PaginaResponseDTO;

@Service
@RequiredArgsConstructor
public class ProductoServiceImpl implements ProductoService {
    private static final Logger logger = LoggerFactory.getLogger(ProductoServiceImpl.class);
    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;

    @Override
    @Transactional
    public ProductoResponseDTO create(ProductoRequestDTO request) {
        String nombre = request.getNombre().trim();
        request.setNombre(nombre);

        if (productoRepository.existsByNombreIgnoreCase(nombre))
            throw new ReglaNegocioException("Ya existe un producto con el nombre " + nombre);

        var categoriaId = request.getCategoriaId();
        Categoria categoria = categoriaRepository.findByIdForUpdate(categoriaId).orElseThrow(() ->
                new RecursosNoEncontradosException("Categoria no encontrada con id: " + categoriaId)
        );

        validarCategoriaActiva(categoria);

        //CREATE
        Producto producto = new Producto();

        ProductoMapper.ConvertFromRequest(producto, request, categoria);
        productoRepository.save(producto);

        return ProductoMapper.ConvertToResponse(producto);
    }

    @Override
    @Transactional
    public ProductoResponseDTO update(Long id, ProductoRequestDTO request) {
        request.setNombre(request.getNombre().trim());

        var categoriaId = request.getCategoriaId();
        Categoria categoria = categoriaRepository.findByIdForUpdate(categoriaId).orElseThrow(() ->
                new RecursosNoEncontradosException("Categoria no encontrada con id: " + categoriaId)
        );

        //UPDATE
        Producto producto = productoRepository.findById(id).orElseThrow(() ->
                new RecursosNoEncontradosException("Producto no encontrado con id: " + id)
        );

        validarCategoriaActiva(categoria);
        if (productoRepository.existsByNombreIgnoreCaseAndIdNot(request.getNombre(), id)) {
            throw new ReglaNegocioException("Ya existe un producto con el nombre " + request.getNombre());
        }

        ProductoMapper.ConvertFromRequest(producto, request, categoria);
        productoRepository.save(producto);

        return ProductoMapper.ConvertToResponse(producto);
    }

    private void validarCategoriaActiva(Categoria categoria) {
        if (!Boolean.TRUE.equals(categoria.getEstado())) {
            throw new ReglaNegocioException("La categoría elegida está inactiva. Elija una categoría activa.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoResponseDTO read(Long id) {
        var optProducto = productoRepository.findById(id);
        if (optProducto.isEmpty())
            throw new RecursosNoEncontradosException("Producto no encontrado con id: " + id);

        return ProductoMapper.ConvertToResponse(optProducto.get());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Producto producto = productoRepository.findById(id).orElseThrow(() ->
                new RecursosNoEncontradosException("Producto no encontrado con id: " + id));
        if (!Boolean.TRUE.equals(producto.getEstado())) {
            throw new ReglaNegocioException("El producto ya se encuentra inactivo.");
        }
        producto.setEstado(false);
        productoRepository.save(producto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponseDTO> readAll() {
        return productoRepository.findAll()
                .stream()
                .map(ProductoMapper::ConvertToResponse)
                .toList();
    }
    @Override
    @Transactional(readOnly = true)
    public PaginaResponseDTO<ProductoResponseDTO> listar(
            int pagina, int tamanio, String ordenarPor, String direccion) {
        if (pagina < 0 || tamanio < 1 || tamanio > 100) {
            throw new ReglaNegocioException("La pagina debe ser mayor o igual a 0 y el tamano debe estar entre 1 y 100.");
        }
        if (ordenarPor == null || !Set.of("id", "nombre", "precio", "stock").contains(ordenarPor)) {
            throw new ReglaNegocioException("El campo de orden no esta permitido.");
        }
        if (!"asc".equalsIgnoreCase(direccion) && !"desc".equalsIgnoreCase(direccion)) {
            throw new ReglaNegocioException("La direccion debe ser asc o desc.");
        }
        Sort orden = Sort.by(Sort.Direction.fromString(direccion), ordenarPor);
        if (!"id".equals(ordenarPor)) orden = orden.and(Sort.by("id"));
        var resultado = productoRepository.findAll(PageRequest.of(pagina, tamanio, orden))
                .map(ProductoMapper::ConvertToResponse);
        return new PaginaResponseDTO<>(resultado.getContent(), resultado.getNumber(),
                resultado.getSize(), resultado.getTotalElements(), resultado.getTotalPages(), resultado.isLast());
    }

}

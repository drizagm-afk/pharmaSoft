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
import java.util.Optional;

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
        Categoria categoria = categoriaRepository.findById(categoriaId).orElseThrow(() ->
                new RecursosNoEncontradosException("Categoria no encontrada con id: " + categoriaId)
        );

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
        Categoria categoria = categoriaRepository.findById(categoriaId).orElseThrow(() ->
                new RecursosNoEncontradosException("Categoria no encontrada con id: " + categoriaId)
        );

        //UPDATE
        Producto producto = productoRepository.findById(id).orElseThrow(() ->
                new RecursosNoEncontradosException("Producto no encontrado con id: " + id)
        );

        ProductoMapper.ConvertFromRequest(producto, request, categoria);
        productoRepository.save(producto);

        return ProductoMapper.ConvertToResponse(producto);
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
        if (!productoRepository.existsById(id))
            throw new RecursosNoEncontradosException("Producto no encontrado con id: " + id);

        productoRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponseDTO> readAll() {
        return productoRepository.findAll()
                .stream()
                .map(ProductoMapper::ConvertToResponse)
                .toList();
    }
}

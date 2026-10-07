package com.edu.upeu.PharmaBackend.service.impl;

import com.edu.upeu.PharmaBackend.dto.CategoriaResponseDTO;
import com.edu.upeu.PharmaBackend.dto.CategoriaRequestDTO;
import com.edu.upeu.PharmaBackend.entity.Categoria;
import com.edu.upeu.PharmaBackend.exception.RecursosNoEncontradosException;
import com.edu.upeu.PharmaBackend.exception.ReglaNegocioException;
import com.edu.upeu.PharmaBackend.mapper.CategoriaMapper;
import com.edu.upeu.PharmaBackend.repository.CategoriaRepository;
import com.edu.upeu.PharmaBackend.repository.ProductoRepository;
import com.edu.upeu.PharmaBackend.service.service.CategoriaService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CategoriaServiceImpl implements CategoriaService {
    private static final Logger logger = LoggerFactory.getLogger(CategoriaServiceImpl.class);
    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;

    @Override
    @Transactional
    public CategoriaResponseDTO create(CategoriaRequestDTO request) {
        String nombre = request.getNombre().trim();
        request.setNombre(nombre);

        if (categoriaRepository.existsByNombreIgnoreCase(nombre))
            throw new ReglaNegocioException("Ya existe una categoria con el nombre " + nombre);

        //CREATE
        Categoria categoria = new Categoria();

        CategoriaMapper.ConvertFromRequest(categoria, request);
        categoriaRepository.save(categoria);

        return CategoriaMapper.ConvertToResponse(categoria);
    }

    @Override
    @Transactional
    public CategoriaResponseDTO update(Long id, CategoriaRequestDTO request) {
        request.setNombre(request.getNombre().trim());

        //UPDATE
        Categoria categoria = categoriaRepository.findByIdForUpdate(id).orElseThrow(() ->
            new RecursosNoEncontradosException("Categoria no encontrada con id: " + id)
        );

        if (!Boolean.TRUE.equals(request.getEstado())
                && productoRepository.existsByCategoriaIdAndEstadoTrue(id)) {
            throw new ReglaNegocioException("No se puede desactivar la categoría porque tiene productos activos. Reasigne o dé de baja esos productos antes de intentarlo de nuevo.");
        }

        CategoriaMapper.ConvertFromRequest(categoria, request);
        categoriaRepository.save(categoria);

        return CategoriaMapper.ConvertToResponse(categoria);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoriaResponseDTO read(Long id) {
        var optCategoria = categoriaRepository.findById(id);
        if (optCategoria.isEmpty())
            throw new RecursosNoEncontradosException("Categoria no encontrada con id: " + id);

        return CategoriaMapper.ConvertToResponse(optCategoria.get());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!categoriaRepository.existsById(id))
            throw new RecursosNoEncontradosException("Categoria no encontrada con id: " + id);

        if (productoRepository.existsByCategoriaId(id))
            throw new ReglaNegocioException("No se puede eliminar la categoría porque tiene productos asociados. Reasigne esos productos a otra categoría antes de intentarlo de nuevo.");

        categoriaRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaResponseDTO> readAll() {
        return categoriaRepository.findAll()
                .stream()
                .map(CategoriaMapper::ConvertToResponse)
                .toList();
    }
}

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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

@Service
@RequiredArgsConstructor
public class CategoriaServiceImpl implements CategoriaService {
    private static final Logger logger = LoggerFactory.getLogger(CategoriaServiceImpl.class);
    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;

    @Override
    @Transactional
    @CacheEvict(cacheNames = "categorias", allEntries = true)
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
    @CacheEvict(cacheNames = {"categorias", "productos"}, allEntries = true)
    public CategoriaResponseDTO update(Long id, CategoriaRequestDTO request) {
        request.setNombre(request.getNombre().trim());

        //UPDATE
        Categoria categoria = categoriaRepository.findById(id).orElseThrow(() ->
            new RecursosNoEncontradosException("Categoria no encontrada con id: " + id)
        );

        CategoriaMapper.ConvertFromRequest(categoria, request);
        categoriaRepository.saveAndFlush(categoria);

        return CategoriaMapper.ConvertToResponse(categoria);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "categorias", key = "'id-' + #p0")
    public CategoriaResponseDTO read(Long id) {
        var optCategoria = categoriaRepository.findById(id);
        if (optCategoria.isEmpty())
            throw new RecursosNoEncontradosException("Categoria no encontrada con id: " + id);

        return CategoriaMapper.ConvertToResponse(optCategoria.get());
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "categorias", allEntries = true)
    public void delete(Long id) {
        if (!categoriaRepository.existsById(id))
            throw new RecursosNoEncontradosException("Categoria no encontrada con id: " + id);

        if (productoRepository.existsByCategoriaId(id)) {
            throw new ReglaNegocioException("No se puede eliminar una categoría con productos asociados. Puede desactivarla desde Editar.");
        }
        categoriaRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "categorias", key = "'todas'")
    public List<CategoriaResponseDTO> readAll() {
        long inicio = System.currentTimeMillis();
        logger.info("Inicio listar categorias");
        var resultado = categoriaRepository.findAll()
                .stream()
                .map(CategoriaMapper::ConvertToResponse)
                .toList();
        logger.info("Fin listar categorias | filas={} | duracionMs={}", resultado.size(),
                System.currentTimeMillis() - inicio);
        return resultado;
    }
}

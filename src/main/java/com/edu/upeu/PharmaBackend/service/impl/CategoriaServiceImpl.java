package com.edu.upeu.PharmaBackend.service.impl;

import com.edu.upeu.PharmaBackend.entity.Categoria;
import com.edu.upeu.PharmaBackend.repository.CategoriaRepository;
import com.edu.upeu.PharmaBackend.service.service.CategoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CategoriaServiceImpl implements CategoriaService {
    private final CategoriaRepository categoriaRepository;

    @Override
    public Categoria create(Categoria categoria) {
        return categoriaRepository.save(categoria);
    }

    @Override
    public Categoria update(Categoria categoria) {
        return categoriaRepository.save(categoria);
    }

    @Override
    public Optional<Categoria> read(Long id) {
        return categoriaRepository.findById(id);
    }

    @Override
    public void delete(Long id) {
        categoriaRepository.deleteById(id);
    }

    @Override
    public Iterable<Categoria> readAll() {
        return categoriaRepository.findAll();
    }
}

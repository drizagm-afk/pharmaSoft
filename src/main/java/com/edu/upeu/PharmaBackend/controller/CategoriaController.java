package com.edu.upeu.PharmaBackend.controller;

import com.edu.upeu.PharmaBackend.entity.Categoria;
import com.edu.upeu.PharmaBackend.service.service.CategoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categorias")
@RequiredArgsConstructor
public class CategoriaController {
    private final CategoriaService categoriaService;

    //Listado de Categorias
    @GetMapping
    public Iterable<Categoria> getCategorias(){
        return categoriaService.readAll();
    }
    @GetMapping("/{id}")
    public Categoria getCategoria(@PathVariable Long id){
        return categoriaService.read(id).get();
    }
}
package com.edu.upeu.PharmaBackend.mapper;

import com.edu.upeu.PharmaBackend.dto.CategoriaResponseDTO;
import com.edu.upeu.PharmaBackend.dto.CategoriaRequestDTO;
import com.edu.upeu.PharmaBackend.entity.Categoria;

public class CategoriaMapper {
    public static CategoriaResponseDTO ConvertToResponse(Categoria categoria){
        return new CategoriaResponseDTO(
                categoria.getId(),
                categoria.getNombre(),
                categoria.getDescripcion(),
                categoria.getEstado(),
                categoria.getFechaCreacion(),
                categoria.getFechaModificacion()
        );
    }

    public static Categoria ConvertFromRequest(Categoria categoria, CategoriaRequestDTO request){
        categoria.setNombre(request.getNombre());
        categoria.setDescripcion(request.getDescripcion());
        categoria.setEstado(request.getEstado());
        return categoria;
    }
}

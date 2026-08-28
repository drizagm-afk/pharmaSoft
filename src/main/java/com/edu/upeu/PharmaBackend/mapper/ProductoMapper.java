package com.edu.upeu.PharmaBackend.mapper;

import com.edu.upeu.PharmaBackend.dto.ProductoRequestDTO;
import com.edu.upeu.PharmaBackend.dto.ProductoResponseDTO;
import com.edu.upeu.PharmaBackend.entity.Categoria;
import com.edu.upeu.PharmaBackend.entity.Producto;

public class ProductoMapper {
    public static ProductoResponseDTO ConvertToResponse(Producto producto){
        return new ProductoResponseDTO(
                producto.getId(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getPrecio(),
                producto.getStock(),
                producto.getCategoria().getId(),
                producto.getCategoria().getNombre(),
                producto.getEstado(),
                producto.getFechaCreacion(),
                producto.getFechaModificacion()
        );
    }

    public static Producto ConvertFromRequest(Producto producto, ProductoRequestDTO request, Categoria categoria){
        producto.setNombre(request.getNombre());
        producto.setDescripcion(request.getDescripcion());
        producto.setPrecio(request.getPrecio());
        producto.setStock(request.getStock());
        producto.setCategoria(categoria);
        producto.setEstado(request.getEstado());
        return producto;
    }
}

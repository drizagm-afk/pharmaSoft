package com.edu.upeu.PharmaBackend.mapper;

import com.edu.upeu.PharmaBackend.dto.DetalleVentaResponseDTO;
import com.edu.upeu.PharmaBackend.dto.VentaResponseDTO;
import com.edu.upeu.PharmaBackend.entity.Venta;

import java.util.List;

public class VentaMapper {
    public static VentaResponseDTO ConvertToResponse(Venta venta) {
        List<DetalleVentaResponseDTO> detalles =
                venta.getDetalles()
                        .stream()
                        .map(detalle ->
                                new DetalleVentaResponseDTO(
                                        detalle.getProducto().getId(),
                                        detalle.getProducto().getNombre(),
                                        detalle.getCantidad(),
                                        detalle.getPrecio(),
                                        detalle.getSubtotal()
                                )
                        ).toList();

        String clienteNombre = venta.getCliente().getNombres() + " " + venta.getCliente().getApellidos();

        return new VentaResponseDTO(
                venta.getId(),
                venta.getFecha(),
                venta.getCliente().getId(),
                clienteNombre,
                venta.getEstado().name(),
                venta.getTotal(),
                detalles
        );
    }
}

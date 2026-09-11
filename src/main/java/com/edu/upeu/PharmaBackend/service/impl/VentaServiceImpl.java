package com.edu.upeu.PharmaBackend.service.impl;

import com.edu.upeu.PharmaBackend.enums.EstadoVenta;
import com.edu.upeu.PharmaBackend.exception.RecursosNoEncontradosException;
import com.edu.upeu.PharmaBackend.mapper.VentaMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.edu.upeu.PharmaBackend.dto.DetalleVentaRequestDTO;
import com.edu.upeu.PharmaBackend.dto.VentaRequestDTO;
import com.edu.upeu.PharmaBackend.dto.VentaResponseDTO;
import com.edu.upeu.PharmaBackend.entity.Cliente;
import com.edu.upeu.PharmaBackend.entity.DetalleVenta;
import com.edu.upeu.PharmaBackend.entity.Producto;
import com.edu.upeu.PharmaBackend.entity.Venta;
import com.edu.upeu.PharmaBackend.exception.ReglaNegocioException;
import com.edu.upeu.PharmaBackend.repository.ClienteRepository;
import com.edu.upeu.PharmaBackend.repository.ProductoRepository;
import com.edu.upeu.PharmaBackend.repository.VentaRepository;
import com.edu.upeu.PharmaBackend.service.service.VentaService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class VentaServiceImpl implements VentaService {
    private static final Logger logger = LoggerFactory.getLogger(VentaServiceImpl.class);
    private final VentaRepository ventaRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;

    @Override
    @Transactional
    public VentaResponseDTO create(VentaRequestDTO request) {
        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new RecursosNoEncontradosException("Cliente no encontrado con id: " + request.getClienteId()));

        if (!Boolean.TRUE.equals(cliente.getEstado())) {
            throw new ReglaNegocioException("No se puede registrar una venta para un cliente inactivo");
        }
        Venta venta = new Venta();

        venta.setCliente(cliente);
        venta.setFecha(LocalDateTime.now());
        venta.setEstado(EstadoVenta.REGISTRADA);

        BigDecimal total = BigDecimal.ZERO;

        for (DetalleVentaRequestDTO item : request.getDetalles()) {
            Producto producto = productoRepository.findById(item.getProductoId()).orElseThrow(() ->
                    new RecursosNoEncontradosException("Producto no encontrado con id: " + item.getProductoId()));

            if (!Boolean.TRUE.equals(producto.getEstado())) {
                throw new ReglaNegocioException("El producto " + producto.getNombre() + " se encuentra inactivo");
            }

            if (producto.getStock() < item.getCantidad()) {

                throw new ReglaNegocioException("Stock insuficiente para " + producto.getNombre() + ". Disponible: " + producto.getStock()
                        + ", solicitado: " + item.getCantidad());
            }

            BigDecimal subtotal = producto.getPrecio().multiply(BigDecimal.valueOf(item.getCantidad()));

            DetalleVenta detalle = new DetalleVenta();

            detalle.setProducto(producto);
            detalle.setCantidad(item.getCantidad());
            detalle.setPrecio(producto.getPrecio());
            detalle.setSubtotal(subtotal);

            venta.agregarDetalle(detalle);

            total = total.add(subtotal);

            producto.setStock(producto.getStock() - item.getCantidad());
        }

        venta.setTotal(total);

        Venta guardada = ventaRepository.save(venta);

        return VentaMapper.ConvertToResponse(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public VentaResponseDTO read(Long id) {
        Venta venta = ventaRepository.findById(id).orElseThrow(() ->
                new RecursosNoEncontradosException("Venta no encontrada con id: " + id));
        return VentaMapper.ConvertToResponse(venta);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VentaResponseDTO> readAll() {
        return ventaRepository.findAll().stream().map(VentaMapper::ConvertToResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VentaResponseDTO> buscar(
            Long clienteId,
            EstadoVenta estado,
            LocalDate desde,
            LocalDate hasta,
            String ordenarPor,
            String direccion
    ) {
        long inicio = System.currentTimeMillis();

        logger.info("Inicio buscar ventas | clienteId={} | estado={} | "
                        + "desde={} | hasta={} | ordenarPor={} | direccion={}",
                clienteId, estado, desde, hasta, ordenarPor, direccion);

        if (desde != null
                && hasta != null
                && desde.isAfter(hasta)) {

            throw new ReglaNegocioException(
                    "El rango de fechas es inválido: 'desde' ("
                            + desde
                            + ") es posterior a 'hasta' ("
                            + hasta + ")");
        }

        Sort sort = construirSort(ordenarPor, direccion);

        LocalDateTime desdeHora = (desde == null)
                ? null
                : desde.atStartOfDay();

        LocalDateTime hastaHora = (hasta == null)
                ? null
                : hasta.atTime(LocalTime.MAX);

        List<VentaResponseDTO> resultado =
                ventaRepository
                        .buscar(clienteId, estado, desdeHora, hastaHora, sort)
                        .stream()
                        .map(VentaMapper::ConvertToResponse)
                        .toList();

        logger.info("Fin buscar ventas | clienteId={} | estado={} | "
                        + "desde={} | hasta={} | orden={} {} | "
                        + "filas={} | duracionMs={}",
                clienteId, estado, desde, hasta, ordenarPor, direccion,
                resultado.size(),
                System.currentTimeMillis() - inicio);

        return resultado;
    }

    //>>>> INTERNAL
    public static final String ORDEN_POR_DEFECTO = "fecha";
    public static final Set<String> CAMPOS_ORDENABLES = Set.of("id", "fecha", "total", "estado");

    private Sort construirSort(String ordenarPor, String direccion) {
        String campo = (ordenarPor == null || ordenarPor.isBlank())
                ? ORDEN_POR_DEFECTO
                : ordenarPor.trim();

        if (!CAMPOS_ORDENABLES.contains(campo)) {
            throw new ReglaNegocioException(
                    "El campo de ordenamiento '"
                            + campo
                            + "' no está permitido. Campos válidos: "
                            + CAMPOS_ORDENABLES);
        }

        String sentido = (direccion == null || direccion.isBlank())
                ? "desc"
                : direccion.trim();

        if (!sentido.equalsIgnoreCase("asc")
                && !sentido.equalsIgnoreCase("desc")) {
            throw new ReglaNegocioException(
                    "La dirección de ordenamiento '" + sentido +
                    "' no está permitida. Valores válidos: asc, desc"
            );
        }

        return sentido.
                equalsIgnoreCase("asc")
                ? Sort.by(campo).ascending()
                : Sort.by(campo).descending();
    }
}
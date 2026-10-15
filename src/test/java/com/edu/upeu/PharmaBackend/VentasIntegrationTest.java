package com.edu.upeu.PharmaBackend;

import com.edu.upeu.PharmaBackend.dto.*;
import com.edu.upeu.PharmaBackend.entity.*;
import com.edu.upeu.PharmaBackend.enums.EstadoVenta;
import com.edu.upeu.PharmaBackend.exception.ReglaNegocioException;
import com.edu.upeu.PharmaBackend.repository.*;
import com.edu.upeu.PharmaBackend.service.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Opt-in Oracle checks. Every test owns its fixtures and removes only those fixtures. */
@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.security.test.context.support.WithMockUser(authorities = {
        "CATALOGO_LEER", "CATALOGO_ESCRIBIR", "CLIENTES_LEER", "CLIENTES_ESCRIBIR",
        "VENTAS_LEER", "VENTAS_REGISTRAR", "REPORTES_VER"})
@EnabledIfSystemProperty(named = "pharma.integration", matches = "true")
class VentasIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired VentaService ventas;
    @Autowired ReporteService reportes;
    @Autowired ClienteService clienteService;
    @Autowired ProductoService productoService;
    @Autowired CategoriaService categoriaService;
    @Autowired VentaRepository ventaRepository;
    @Autowired ClienteRepository clientes;
    @Autowired ProductoRepository productos;
    @Autowired CategoriaRepository categorias;
    @Autowired TransactionTemplate tx;
    Long clienteId, categoriaId, primeroId, segundoId;

    @BeforeEach
    void preparar() {
        tx.executeWithoutResult(status -> {
            categoriaId = categoriaService.create(new CategoriaRequestDTO(
                    "TEST ventas " + System.nanoTime(), null, true)).getId();
            Categoria categoria = categorias.findById(categoriaId).orElseThrow();
            clienteId = clienteService.create(new ClienteRequestDTO("99999999", "Prueba", "Automatizada",
                    "prueba@example.invalid", null, null, true)).getId();
            primeroId = producto(categoria, "TEST primero", "2.50", 5);
            segundoId = producto(categoria, "TEST segundo", "3.75", 1);
        });
    }

    private Long producto(Categoria categoria, String nombre, String precio, int stock) {
        return productoService.create(new ProductoRequestDTO(nombre, null, new BigDecimal(precio),
                stock, categoria.getId(), true)).getId();
    }

    @AfterEach
    void limpiar() {
        if (clienteId == null) return;
        tx.executeWithoutResult(status -> {
            var propias = ventaRepository.buscar(clienteId, null, null, null,
                    org.springframework.data.domain.Pageable.unpaged());
            ventaRepository.deleteAll(propias.getContent());
            ventaRepository.flush();
            productos.deleteAllById(List.of(primeroId, segundoId));
            productos.flush();
            clientes.deleteById(clienteId);
            categorias.deleteById(categoriaId);
        });
    }

    private VentaRequestDTO pedido(DetalleVentaRequestDTO... detalles) {
        return new VentaRequestDTO(clienteId, List.of(detalles));
    }

    private DetalleVentaRequestDTO linea(Long id, int cantidad) {
        return new DetalleVentaRequestDTO(id, cantidad);
    }

    @Test
    void apiRechazaFraccionesYDetallesNulosSinModificarStock() throws Exception {
        mvc.perform(post("/api/v1/ventas").contentType("application/json").content(
                "{\"clienteId\":" + clienteId + ",\"detalles\":[{\"productoId\":" + primeroId + ",\"cantidad\":1.5}]}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.trace").doesNotExist());
        mvc.perform(post("/api/v1/ventas").contentType("application/json").content(
                "{\"clienteId\":" + clienteId + ",\"detalles\":[null]}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.validationErrors").isMap());
        assertThat(productos.findById(primeroId).orElseThrow().getStock()).isEqualTo(5);
    }

    @Test
    void apiRegistraVentaYLaConsultaPorCliente() throws Exception {
        mvc.perform(post("/api/v1/ventas").contentType("application/json").content(
                "{\"clienteId\":" + clienteId + ",\"detalles\":[{\"productoId\":" + primeroId + ",\"cantidad\":2}]}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.total").value(5.0));
        mvc.perform(get("/api/v1/ventas/buscar").param("clienteId", clienteId.toString()).param("tamanio", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].detalles[0].cantidad").value(2));
    }

    @Test
    void catalogosPermitenLeerActualizarYDarDeBaja() {
        assertThat(categoriaService.read(categoriaId).getEstado()).isTrue();
        assertThatThrownBy(() -> categoriaService.delete(categoriaId)).isInstanceOf(ReglaNegocioException.class);
        assertThat(clienteService.read(clienteId).getNombres()).isEqualTo("Prueba");
        var producto = new ProductoRequestDTO("TEST primero", null, new BigDecimal("4.35"), 5, categoriaId, true);
        productoService.update(primeroId, producto);
        assertThat(productoService.read(primeroId).getPrecio()).isEqualByComparingTo("4.35");
        productoService.delete(primeroId);
        assertThat(productoService.read(primeroId).getEstado()).isFalse();
        assertThatThrownBy(() -> productoService.delete(primeroId)).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> ventas.create(pedido(linea(primeroId, 1))))
                .isInstanceOf(ReglaNegocioException.class);
        clienteService.delete(clienteId);
        assertThat(clienteService.read(clienteId).getEstado()).isFalse();
        assertThatThrownBy(() -> clienteService.delete(clienteId)).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void calculaImportesYDescuentaStock() {
        var venta = ventas.create(pedido(linea(primeroId, 2), linea(segundoId, 1)));
        assertThat(venta.getTotal()).isEqualByComparingTo("8.75");
        assertThat(venta.getDetalles()).hasSize(2);
        assertThat(ventas.read(venta.getId()).getTotal()).isEqualByComparingTo("8.75");
        assertThat(productos.findById(primeroId).orElseThrow().getStock()).isEqualTo(3);
    }

    @Test
    void revierteTodaLaVentaSiUnaLineaFalla() {
        assertThatThrownBy(() -> ventas.create(pedido(linea(primeroId, 2), linea(segundoId, 2))))
                .isInstanceOf(ReglaNegocioException.class);
        assertThat(productos.findById(primeroId).orElseThrow().getStock()).isEqualTo(5);
        assertThat(ventas.buscar(clienteId, null, null, null, "id", "asc", 0, 10).totalElementos()).isZero();
    }

    @Test
    void lineasRepetidasNoPermitenSobrevender() {
        assertThatThrownBy(() -> ventas.create(pedido(linea(primeroId, 3), linea(primeroId, 3))))
                .isInstanceOf(ReglaNegocioException.class);
        assertThat(productos.findById(primeroId).orElseThrow().getStock()).isEqualTo(5);
    }

    @Test
    void rechazaClienteInactivo() {
        tx.executeWithoutResult(status -> {
            var cliente = clientes.findById(clienteId).orElseThrow();
            cliente.setEstado(false);
        });
        assertThatThrownBy(() -> ventas.create(pedido(linea(primeroId, 1))))
                .isInstanceOf(ReglaNegocioException.class);
        assertThat(productos.findById(primeroId).orElseThrow().getStock()).isEqualTo(5);
    }

    @Test
    void filtraPaginaYReporteExcluyeAnuladas() {
        var primera = ventas.create(pedido(linea(primeroId, 1)));
        ventas.create(pedido(linea(primeroId, 2)));
        tx.executeWithoutResult(status -> ventaRepository.findById(primera.getId()).orElseThrow().setEstado(EstadoVenta.ANULADA));
        var pagina = ventas.buscar(clienteId, EstadoVenta.REGISTRADA, LocalDate.now(), LocalDate.now(), "id", "asc", 0, 1);
        assertThat(pagina.totalElementos()).isEqualTo(1);
        var fila = reportes.ventasPorCategoria(LocalDate.now(), LocalDate.now()).stream()
                .filter(r -> r.categoriaId().equals(categoriaId)).findFirst().orElseThrow();
        assertThat(fila.cantidad()).isEqualTo(2);
        assertThat(fila.total()).isEqualByComparingTo("5.00");
    }

    @Test
    void dosCompradoresCompitenPorLaUltimaUnidad() throws Exception {
        var inicio = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> comprar = () -> {
                inicio.await();
                try {
                    ventas.create(pedido(linea(segundoId, 1)));
                    return true;
                } catch (ReglaNegocioException ex) {
                    return false;
                }
            };
            var a = executor.submit(comprar);
            var b = executor.submit(comprar);
            inicio.countDown();
            assertThat(List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        }
        assertThat(productos.findById(segundoId).orElseThrow().getStock()).isZero();
        assertThat(ventas.buscar(clienteId, null, null, null, "id", "asc", 0, 10).totalElementos()).isEqualTo(1);
    }
}


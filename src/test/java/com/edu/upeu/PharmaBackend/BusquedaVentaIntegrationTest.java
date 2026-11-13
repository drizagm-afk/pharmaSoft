package com.edu.upeu.PharmaBackend;

import com.edu.upeu.PharmaBackend.entity.*;
import com.edu.upeu.PharmaBackend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("dev")
@SpringBootTest(properties = {
    "spring.cache.type=none", "management.health.redis.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:busqueda-venta;MODE=Oracle;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false"})
@AutoConfigureMockMvc
@Transactional
@WithMockUser(authorities = {"CATALOGO_LEER", "CLIENTES_LEER"})
class BusquedaVentaIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ProductoRepository productos;
    @Autowired ClienteRepository clientes;
    @Autowired CategoriaRepository categorias;
    Long productoId, clienteId;

    @BeforeEach
    void prepararVolumen() {
        Categoria categoria = new Categoria(); categoria.setNombre("Cuidado e higiene");
        categorias.save(categoria);
        for (int i = 0; i < 101; i++) {
            producto(categoria, "Alcohol 70% Ref. " + String.format("%03d", i), 10, true);
            cliente("María Elena", "Aguirre Flores", i, true);
        }
        productoId = producto(categoria, "Solución salina 100 ml", 8, true).getId();
        clienteId = cliente("Nicolás", "Zúñiga Salas", 101, true).getId();
        producto(categoria, "Solución salina 250 ml", 0, true);
        producto(categoria, "Solución salina 500 ml", 8, false);
        cliente("Nicolás", "Zúñiga Ramos", 102, false);
        productos.flush(); clientes.flush();
    }

    @Test
    void encuentraCoincidenciasFueraDeLaPrimeraPaginaSinOfrecerInactivosOSinStock() throws Exception {
        assertThat(productos.findAll(PageRequest.of(0, 100, Sort.by("nombre"))))
                .noneMatch(p -> p.getId().equals(productoId));
        assertThat(clientes.findAll(PageRequest.of(0, 100, Sort.by("apellidos"))))
                .noneMatch(c -> c.getId().equals(clienteId));
        mvc.perform(get("/api/v1/productos/buscar").param("texto", "  solucion salina  "))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].id").value(productoId));
        mvc.perform(get("/api/v1/clientes/buscar").param("texto", "zuniga"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].id").value(clienteId));
    }

    @Test
    void buscaCategoriaNombreCompletoYDniConResultadosPaginados() throws Exception {
        mvc.perform(get("/api/v1/productos/buscar").param("texto", "higiene"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tamanio").value(6))
                .andExpect(jsonPath("$.totalElementos").value(102))
                .andExpect(jsonPath("$.contenido.length()").value(6));
        mvc.perform(get("/api/v1/clientes/buscar").param("texto", "nicolas zuniga"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.contenido[0].id").value(clienteId));
        mvc.perform(get("/api/v1/clientes/buscar").param("texto", "22000101"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.contenido[0].id").value(clienteId));
        mvc.perform(get("/api/v1/productos/buscar").param("texto", "_"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(0));
        mvc.perform(get("/api/v1/productos/buscar").param("texto", " "))
                .andExpect(status().isOk()).andExpect(jsonPath("$.contenido.length()").value(0));
    }

    private Producto producto(Categoria categoria, String nombre, int stock, boolean estado) {
        Producto p = new Producto(); p.setCategoria(categoria); p.setNombre(nombre);
        p.setPrecio(new BigDecimal("2.50")); p.setStock(stock); p.setEstado(estado);
        return productos.save(p);
    }

    private Cliente cliente(String nombres, String apellidos, int numero, boolean estado) {
        Cliente c = new Cliente(); c.setNombres(nombres); c.setApellidos(apellidos);
        c.setDni(String.valueOf(22000000 + numero));
        c.setEmail("cliente" + numero + "@example.com"); c.setEstado(estado);
        return clientes.save(c);
    }
}

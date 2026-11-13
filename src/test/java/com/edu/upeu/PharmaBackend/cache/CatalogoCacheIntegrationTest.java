package com.edu.upeu.PharmaBackend.cache;

import com.edu.upeu.PharmaBackend.dto.*;
import com.edu.upeu.PharmaBackend.entity.*;
import com.edu.upeu.PharmaBackend.repository.*;
import com.edu.upeu.PharmaBackend.service.service.*;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

/** Real Redis, isolated H2 data and test-only cache keys; never touches Oracle. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:catalogo-cache;MODE=Oracle;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "logging.level.org.hibernate.stat=OFF", "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=OFF",
        "spring.flyway.enabled=false", "spring.data.redis.database=15",
        "spring.cache.redis.key-prefix=s13-test::"})
@EnabledIfSystemProperty(named = "pharma.cache.integration", matches = "true")
@AutoConfigureMockMvc
class CatalogoCacheIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CategoriaService categorias;
    @Autowired ProductoService productos;
    @Autowired VentaService ventas;
    @Autowired ProductoRepository productoRepository;
    @Autowired ClienteRepository clienteRepository;
    @Autowired CacheManager caches;
    @Autowired StringRedisTemplate redis;
    @Autowired EntityManagerFactory emf;
    Long categoriaId, productoId, clienteId;

    @BeforeEach
    void preparar() {
        limpiar();
        categoriaId = categorias.create(new CategoriaRequestDTO("Categoria-" + UUID.randomUUID(), null, true)).getId();
        productoId = productos.create(producto("Producto-" + UUID.randomUUID(), 10)).getId();
        Cliente cliente = new Cliente();
        cliente.setDni("12345678"); cliente.setNombres("Prueba"); cliente.setApellidos("Cache");
        cliente.setEmail(UUID.randomUUID() + "@test.local"); cliente.setEstado(true);
        clienteId = clienteRepository.saveAndFlush(cliente).getId();
        estadisticas().clear();
    }

    @AfterEach
    void limpiar() {
        caches.getCacheNames().forEach(nombre -> caches.getCache(nombre).clear());
    }

    private org.hibernate.stat.Statistics estadisticas() {
        return emf.unwrap(SessionFactory.class).getStatistics();
    }

    private ProductoRequestDTO producto(String nombre, int stock) {
        return new ProductoRequestDTO(nombre, null, new BigDecimal("2.00"), stock, categoriaId, true);
    }

    @Test
    @WithMockUser(authorities = {"ROLE_ADMIN", "USUARIOS_ADMINISTRAR"})
    void metricasRegistranHitsYSaludIncluyeRedisParaAdmin() throws Exception {
        categorias.readAll(); categorias.readAll();
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.components.redis.status").value("UP"));
        mvc.perform(get("/actuator/metrics/cache.gets").param("tag", "name:categorias", "result:hit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.measurements[0].value", org.hamcrest.Matchers.greaterThan(0.0)));
    }

    @Test
    void segundaListaNoConsultaSqlYRedisTieneTtl() {
        assertThat(categorias.readAll()).isNotEmpty();
        long consultas = estadisticas().getPrepareStatementCount();
        assertThat(consultas).isPositive();
        assertThat(categorias.readAll()).isNotEmpty();
        assertThat(estadisticas().getPrepareStatementCount()).isEqualTo(consultas);
        assertThat(redis.getExpire("s13-test::categorias::todas")).isBetween(1L, 600L);
        categorias.read(categoriaId);
        consultas = estadisticas().getPrepareStatementCount();
        assertThat(categorias.read(categoriaId).getId()).isEqualTo(categoriaId);
        assertThat(categorias.read(categoriaId).getCreadoPor()).isEqualTo("sistema");
        assertThat(estadisticas().getPrepareStatementCount()).isEqualTo(consultas);
    }

    @Test
    void paginasYOrdenesTienenClavesDistintasYSeSerializan() {
        productos.listar(0, 1, "nombre", "asc");
        long consultas = estadisticas().getPrepareStatementCount();
        assertThat(productos.listar(0, 1, "nombre", "asc").contenido()).hasSize(1);
        assertThat(productos.listar(0, 1, "nombre", "asc").contenido().getFirst().getCreadoPor())
                .isEqualTo("sistema");
        assertThat(estadisticas().getPrepareStatementCount()).isEqualTo(consultas);
        productos.listar(0, 2, "nombre", "desc");
        assertThat(redis.hasKey("s13-test::productos::0-1-nombre-asc")).isTrue();
        assertThat(redis.hasKey("s13-test::productos::0-2-nombre-desc")).isTrue();
    }

    @Test
    void renombrarCategoriaInvalidaAmbosCatalogos() {
        categorias.readAll(); productos.read(productoId); productos.listar(0, 10, "nombre", "asc");
        categorias.update(categoriaId, new CategoriaRequestDTO("Renombrada", null, true));
        assertThat(categorias.read(categoriaId).getNombre()).isEqualTo("Renombrada");
        assertThat(productos.read(productoId).getCategoriaNombre()).isEqualTo("Renombrada");
        assertThat(redis.hasKey("s13-test::productos::0-10-nombre-asc")).isFalse();
    }

    @Test
    void altaYBajaDeCategoriaInvalidanLaLista() {
        categorias.readAll();
        Long id = categorias.create(new CategoriaRequestDTO("Temporal-" + UUID.randomUUID(), null, true)).getId();
        assertThat(categorias.readAll()).anyMatch(c -> c.getId().equals(id));
        categorias.delete(id);
        assertThat(categorias.readAll()).noneMatch(c -> c.getId().equals(id));
    }

    @Test
    void escriturasDeProductoInvalidanListasYDetalle() {
        productos.readAll(); productos.read(productoId); productos.listar(0, 10, "nombre", "asc");
        productos.update(productoId, producto("Actualizado", 8));
        assertThat(productos.read(productoId).getStock()).isEqualTo(8);
        assertThat(redis.hasKey("s13-test::productos::todas")).isFalse();
        productos.readAll(); productos.delete(productoId);
        assertThat(productos.read(productoId).getEstado()).isFalse();
        assertThat(redis.hasKey("s13-test::productos::todas")).isFalse();
        productos.readAll(); productos.create(producto("Nuevo-" + UUID.randomUUID(), 3));
        assertThat(redis.hasKey("s13-test::productos::todas")).isFalse();
    }

    @Test
    void ventaConfirmadaInvalidaStockYVentaFallidaRevierteElDescuento() {
        productos.read(productoId); productos.listar(0, 10, "nombre", "asc");
        ventas.create(new VentaRequestDTO(clienteId, List.of(new DetalleVentaRequestDTO(productoId, 2))));
        assertThat(productos.read(productoId).getStock()).isEqualTo(8);
        assertThat(redis.hasKey("s13-test::productos::0-10-nombre-asc")).isFalse();
        assertThatThrownBy(() -> ventas.create(new VentaRequestDTO(clienteId, List.of(
                new DetalleVentaRequestDTO(productoId, 1), new DetalleVentaRequestDTO(productoId, 100)))))
                .hasMessageContaining("Stock insuficiente");
        assertThat(productoRepository.findById(productoId).orElseThrow().getStock()).isEqualTo(8);
        assertThat(productos.read(productoId).getStock()).isEqualTo(8);
    }
}

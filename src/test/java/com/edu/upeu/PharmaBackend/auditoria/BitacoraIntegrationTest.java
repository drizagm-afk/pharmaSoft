package com.edu.upeu.PharmaBackend.auditoria;

import com.edu.upeu.PharmaBackend.dto.*;
import com.edu.upeu.PharmaBackend.entity.*;
import com.edu.upeu.PharmaBackend.repository.*;
import com.edu.upeu.PharmaBackend.service.service.*;
import org.junit.jupiter.api.*;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real commits and rollbacks in isolated H2; no Oracle records are changed. */
@ActiveProfiles("dev")
@SpringBootTest(properties = {
    "spring.cache.type=none", "management.health.redis.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:bitacora;MODE=Oracle;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false",
    "spring.flyway.enabled=false"})
@AutoConfigureMockMvc
class BitacoraIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired TransactionTemplate tx;
    @Autowired AuditoriaEventoRepository registros;
    @Autowired AuditoriaService auditoria;
    @Autowired CategoriaRepository categorias;
    @Autowired ProductoRepository productos;
    @Autowired ClienteRepository clientes;
    @Autowired VentaRepository ventas;
    @Autowired ProductoService productoService;
    @Autowired VentaService ventaService;
    Long categoriaId, productoId, clienteId;

    @BeforeEach
    void preparar() {
        org.springframework.security.test.context.TestSecurityContextHolder.clearContext();
        autenticar("rosa.mendoza");
        tx.executeWithoutResult(status -> {
            Categoria categoria = new Categoria(); categoria.setNombre("Cuidado respiratorio");
            categoriaId = categorias.save(categoria).getId();
            Producto producto = new Producto(); producto.setNombre("Solución salina 100 ml");
            producto.setPrecio(new BigDecimal("2.50")); producto.setStock(10);
            producto.setCategoria(categoria);
            productoId = productos.save(producto).getId();
            Cliente cliente = new Cliente(); cliente.setDni("47812365");
            cliente.setNombres("Lucía"); cliente.setApellidos("Ramos Cutipa");
            cliente.setEmail("lucia.ramos@example.com");
            clienteId = clientes.save(cliente).getId();
        });
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void limpiar() {
        org.springframework.security.test.context.TestSecurityContextHolder.clearContext();
        MDC.clear(); SecurityContextHolder.clearContext();
        tx.executeWithoutResult(status -> {
            registros.deleteAllInBatch();
            ventas.deleteAll(); ventas.flush();
            productos.deleteAllInBatch();
            clientes.deleteAllInBatch();
            categorias.deleteAllInBatch();
        });
    }

    @Test
    void ventaConfirmadaConservaUsuarioDetalleYRequestIdDeLaPeticion() throws Exception {
        var respuesta = mvc.perform(post("/api/v1/ventas")
                .with(user("lucia.ramos").authorities(() -> "VENTAS_REGISTRAR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"clienteId\":" + clienteId + ",\"detalles\":[{\"productoId\":"
                        + productoId + ",\"cantidad\":2}]}"))
                .andExpect(status().isCreated()).andReturn().getResponse();
        var evento = registros.findAll().getFirst();
        assertThat(evento.getAccion()).isEqualTo("VENTA_REGISTRADA");
        assertThat(evento.getUsuario()).isEqualTo("lucia.ramos");
        assertThat(evento.getEntidad()).isEqualTo("Venta");
        assertThat(ventas.existsById(evento.getEntidadId())).isTrue();
        assertThat(evento.getDetalle()).isEqualTo("total=5.00 | items=1");
        assertThat(evento.getRequestId()).isEqualTo(respuesta.getHeader("X-Request-Id"));
        assertThat(evento.getFecha()).isNotNull();
        assertThat(productos.findById(productoId).orElseThrow().getStock()).isEqualTo(8);
    }

    @Test
    void rollbackDeVentaNoDejaEventoNiDescuento() {
        autenticar("lucia.ramos");
        tx.executeWithoutResult(status -> {
            ventaService.create(pedido(2));
            assertThat(registros.count()).isZero();
            status.setRollbackOnly();
        });
        assertThat(ventas.count()).isZero();
        assertThat(registros.count()).isZero();
        assertThat(productos.findById(productoId).orElseThrow().getStock()).isEqualTo(10);
        assertThatThrownBy(() -> ventaService.create(pedido(11)))
                .hasMessageContaining("Stock insuficiente");
        assertThat(registros.count()).isZero();
    }

    @Test
    void precioYBajaPublicanSoloCambiosConfirmados() {
        autenticar("rosa.mendoza");
        productoService.update(productoId, producto("2.500"));
        assertThat(registros.count()).isZero();
        tx.executeWithoutResult(status -> {
            productoService.update(productoId, producto("3.20"));
            status.setRollbackOnly();
        });
        assertThat(registros.count()).isZero();
        productoService.update(productoId, producto("2.80"));
        var precio = registros.findAll().getFirst();
        assertThat(precio.getAccion()).isEqualTo("PRECIO_MODIFICADO");
        assertThat(precio.getDetalle()).isEqualTo("2.50 -> 2.80");
        assertThat(precio.getEntidadId()).isEqualTo(productoId);
        assertThat(productos.findById(productoId).orElseThrow().getModificadoPor())
                .isEqualTo("rosa.mendoza");
        productoService.delete(productoId);
        assertThat(registros.findAll()).anySatisfy(e -> {
            assertThat(e.getAccion()).isEqualTo("PRODUCTO_ELIMINADO");
            assertThat(e.getDetalle()).isEqualTo("Solución salina 100 ml");
        });
        assertThatThrownBy(() -> productoService.delete(productoId)).hasMessageContaining("inactivo");
        assertThat(registros.count()).isEqualTo(2);
    }

    @Test
    void consultaFiltraUsuarioAccionYDiaCompletoConPaginacion() throws Exception {
        var dia = LocalDate.of(2026, 11, 13);
        tx.executeWithoutResult(status -> {
            insertar(dia.atStartOfDay(), "rosa.mendoza", "PRECIO_MODIFICADO");
            insertar(dia.atTime(23, 59, 59, 999_999_000), "rosa.mendoza", "PRECIO_MODIFICADO");
            insertar(dia.plusDays(1).atStartOfDay(), "rosa.mendoza", "PRECIO_MODIFICADO");
            insertar(dia.atTime(12, 0), "lucia.ramos", "LOGIN_EXITOSO");
        });
        mvc.perform(get("/api/v1/auditoria")
                .with(user("admin").authorities(() -> "USUARIOS_ADMINISTRAR"))
                .param("usuario", " ROSA.MENDOZA ").param("accion", " PRECIO_MODIFICADO ")
                .param("desde", dia.toString()).param("hasta", dia.toString())
                .param("pagina", "1").param("tamanio", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.totalElementos").value(2))
                .andExpect(jsonPath("$.totalPaginas").value(2))
                .andExpect(jsonPath("$.ultima").value(true))
                .andExpect(jsonPath("$.contenido[0].fecha").value(dia + "T00:00:00"));
        assertThat(auditoria.buscar(" ", "", null, null, 0, 20).totalElementos()).isEqualTo(4);
        assertThat(auditoria.buscar(null, "LOGIN_EXITOSO", null, null, 0, 20).totalElementos())
                .isEqualTo(1);
        assertThat(auditoria.buscar("inexistente", null, null, null, 0, 20).contenido()).isEmpty();
    }

    @Test
    void consultaExigeAdministracionYValidaParametros() throws Exception {
        mvc.perform(get("/api/v1/auditoria").with(anonymous())).andExpect(status().isUnauthorized());
        for (String rol : List.of("VENDEDOR", "ALMACENERO")) {
            mvc.perform(get("/api/v1/auditoria").with(user("lucia.ramos").roles(rol)))
                    .andExpect(status().isForbidden());
        }
        var admin = user("admin").authorities(() -> "USUARIOS_ADMINISTRAR");
        mvc.perform(get("/api/v1/auditoria").with(admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tamanio").value(20));
        for (String[] parametro : List.of(new String[]{"pagina", "-1"}, new String[]{"tamanio", "101"})) {
            mvc.perform(get("/api/v1/auditoria").with(admin).param(parametro[0], parametro[1]))
                    .andExpect(status().isConflict());
        }
        mvc.perform(get("/api/v1/auditoria").with(admin).param("desde", "2026-11-14")
                .param("hasta", "2026-11-13")).andExpect(status().isConflict());
        mvc.perform(get("/api/v1/auditoria").with(admin).param("desde", "ayer"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registroIndependienteSobreviveAlRollbackExterior() {
        tx.executeWithoutResult(status -> {
            auditoria.registrar(EventoAuditoria.login("LOGIN_FALLIDO", "rosa.mendoza"));
            status.setRollbackOnly();
        });
        assertThat(registros.findAll()).singleElement().satisfies(e -> {
            assertThat(e.getAccion()).isEqualTo("LOGIN_FALLIDO");
            assertThat(e.getDetalle()).isNull();
        });
    }

    private void insertar(LocalDateTime fecha, String usuario, String accion) {
        AuditoriaEvento e = new AuditoriaEvento();
        e.setFecha(fecha); e.setUsuario(usuario); e.setAccion(accion); e.setEntidad("Producto");
        registros.save(e);
    }

    private ProductoRequestDTO producto(String precio) {
        return new ProductoRequestDTO("Solución salina 100 ml", null,
                new BigDecimal(precio), 10, categoriaId, true);
    }

    private VentaRequestDTO pedido(int cantidad) {
        return new VentaRequestDTO(clienteId, List.of(new DetalleVentaRequestDTO(productoId, cantidad)));
    }

    private void autenticar(String usuario) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(usuario, null, List.of()));
    }
}

package com.edu.upeu.PharmaBackend.security;

import com.edu.upeu.PharmaBackend.entity.*;
import com.edu.upeu.PharmaBackend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real security chain and JPA services against an isolated, rolled-back database. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:security;MODE=Oracle;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false",
        "spring.flyway.enabled=false"})
@AutoConfigureMockMvc
@Transactional
class SeguridadIntegrationTest {
    private static final String HASH = new BCryptPasswordEncoder().encode("Password2026*");
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired RolRepository roles;
    @Autowired EmpleadoRepository empleados;
    @Autowired CategoriaRepository categorias;
    @Autowired ProductoRepository productos;
    @Autowired ClienteRepository clientes;
    @Autowired JwtService jwt;
    @Autowired UsuarioDetailsService details;
    Long empleadoLibre, vendedorRol;

    @BeforeEach
    void preparar() {
        var admin = roles.save(new Rol(null, "ADMIN", null));
        var vendedor = roles.save(new Rol(null, "VENDEDOR", null));
        var almacen = roles.save(new Rol(null, "ALMACENERO", null));
        vendedorRol = vendedor.getId();
        cuenta("admin", true, admin);
        cuenta("vendedor", true, vendedor);
        cuenta("almacen", true, almacen);
        cuenta("supervisor", true, vendedor, almacen);
        cuenta("inactivo", false, vendedor);
        empleadoLibre = empleado("Libre", 6).getId();
    }

    private Empleado empleado(String nombre, int numero) {
        var e = new Empleado();
        e.setDni("8000000" + numero); e.setNombres(nombre); e.setApellidos("Prueba");
        e.setCargo("Caja"); e.setEmail(nombre + "@example.invalid"); e.setEstado(true);
        return empleados.save(e);
    }

    private void cuenta(String nombre, boolean activo, Rol... asignados) {
        var u = new Usuario();
        u.setUsername(nombre); u.setPassword(HASH); u.setEstado(activo);
        u.setEmpleado(empleado(nombre, (int) usuarios.count() + 1));
        u.setRoles(new HashSet<>(List.of(asignados))); usuarios.saveAndFlush(u);
    }

    private String bearer(String nombre) {
        return "Bearer " + jwt.generarToken(details.loadUserByUsername(nombre));
    }

    @Test
    void rutasPublicasYRechazoSinToken() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/health")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/productos")).andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.message").value("Debe iniciar sesión para acceder a este recurso"))
                .andExpect(jsonPath("$.path").value("/api/v1/productos"));
    }

    @ParameterizedTest
    @CsvSource({"admin,incorrecta", "inactivo,Password2026*", "desconocido,Password2026*"})
    void credencialesInvalidasMismoMensaje(String usuario, String password) throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + usuario + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Usuario o contraseña incorrectos"));
    }

    @Test
    void loginYPerfilSinPassword() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\" VENDEDOR \",\"password\":\"Password2026*\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiraEnSegundos").value(3600))
                .andExpect(jsonPath("$.roles[0]").value("VENDEDOR"))
                .andExpect(jsonPath("$.token").isString()).andExpect(jsonPath("$.password").doesNotExist());
        mvc.perform(get("/api/v1/auth/me").header("Authorization", bearer("vendedor")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value("vendedor"))
                .andExpect(jsonPath("$.password").doesNotExist()).andExpect(jsonPath("$.empleadoNombre").isString());
    }

    @Test
    void loginVacioEs400() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"\",\"password\":\"x\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.validationErrors.username").exists());
    }

    @ParameterizedTest
    @CsvSource({
            "vendedor,/api/v1/productos,200", "almacen,/api/v1/productos,200",
            "vendedor,/api/v1/clientes,200", "almacen,/api/v1/clientes,403",
            "vendedor,/api/v1/ventas/buscar,200", "almacen,/api/v1/ventas/buscar,403",
            "admin,/api/v1/reportes/ventas-por-categoria,200",
            "vendedor,/api/v1/reportes/ventas-por-categoria,403",
            "almacen,/api/v1/reportes/ventas-por-categoria,403",
            "supervisor,/api/v1/reportes/ventas-por-categoria,403",
            "admin,/api/v1/usuarios,200", "vendedor,/api/v1/usuarios,403",
            "supervisor,/api/v1/empleados,403", "admin,/api/v1/roles,200"})
    void permisosDeLectura(String nombre, String ruta, int codigo) throws Exception {
        mvc.perform(get(ruta).header("Authorization", bearer(nombre))).andExpect(status().is(codigo));
    }

    @Test
    void escrituraDenegadaAntesDeValidarElCuerpo() throws Exception {
        mvc.perform(delete("/api/v1/productos/999999").header("Authorization", bearer("vendedor")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No tiene permisos para realizar esta operación"));
        mvc.perform(post("/api/v1/ventas").header("Authorization", bearer("almacen")))
                .andExpect(status().isForbidden());
    }

    @Test
    void supervisorVendeYActualizaCatalogo() throws Exception {
        var c = new Categoria(); c.setNombre("Prueba"); c.setEstado(true); categorias.save(c);
        var p = new Producto(); p.setNombre("Producto"); p.setCategoria(c); p.setPrecio(new BigDecimal("12.50"));
        p.setStock(5); p.setEstado(true); productos.saveAndFlush(p);
        var cliente = new Cliente(); cliente.setDni("90000001"); cliente.setNombres("Cliente");
        cliente.setApellidos("Prueba"); cliente.setEmail("cliente@example.invalid"); clientes.saveAndFlush(cliente);
        mvc.perform(put("/api/v1/productos/" + p.getId()).header("Authorization", bearer("supervisor"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Producto\",\"precio\":12.5,\"stock\":5,\"categoriaId\":" + c.getId() + ",\"estado\":true}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/ventas").header("Authorization", bearer("supervisor"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"clienteId\":" + cliente.getId()
                        + ",\"detalles\":[{\"productoId\":" + p.getId() + ",\"cantidad\":2}]}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.total").value(25.0));
        assertThat(productos.findById(p.getId()).orElseThrow().getStock()).isEqualTo(3);
    }

    private String alta(String nombre, String password, Set<Long> ids) {
        return "{\"username\":\"" + nombre + "\",\"password\":\"" + password
                + "\",\"empleadoId\":" + empleadoLibre + ",\"rolIds\":" + ids + "}";
    }

    @Test
    void altaBcryptRelacionesYDuplicado() throws Exception {
        mvc.perform(post("/api/v1/usuarios").header("Authorization", bearer("admin"))
                .contentType(MediaType.APPLICATION_JSON).content(alta("nueva", "Cajera2026", Set.of(vendedorRol))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.roles[0]").value("VENDEDOR"))
                .andExpect(jsonPath("$.password").doesNotExist());
        var u = usuarios.findByUsernameIgnoreCase("nueva").orElseThrow();
        assertThat(new BCryptPasswordEncoder().matches("Cajera2026", u.getPassword())).isTrue();
        assertThat(u.getEmpleado().getId()).isEqualTo(empleadoLibre);
        mvc.perform(post("/api/v1/usuarios").header("Authorization", bearer("admin"))
                .contentType(MediaType.APPLICATION_JSON).content(alta("otra", "Cajera2026", Set.of(vendedorRol))))
                .andExpect(status().isConflict());
    }

    @Test
    void rolesInexistentesYPasswordUtf8() throws Exception {
        mvc.perform(post("/api/v1/usuarios").header("Authorization", bearer("admin"))
                .contentType(MediaType.APPLICATION_JSON).content(alta("nueva", "Cajera2026", Set.of(vendedorRol, 999999L))))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/usuarios").header("Authorization", bearer("admin"))
                .contentType(MediaType.APPLICATION_JSON).content(alta("nueva", "a1" + "ñ".repeat(36), Set.of(vendedorRol))))
                .andExpect(status().isConflict());
    }

    @Test
    void tokenExpiradoAlteradoYCuentaDeshabilitada() throws Exception {
        var vendedor = details.loadUserByUsername("vendedor");
        String key = "w9Z6dP/dzykNH/uiI3Z/KGjBlP0H4Ji+Tuq4fm2cTNuLD4jWafu1gBL74V2SJEvy";
        String vencido = new JwtService(key, -1).generarToken(vendedor);
        mvc.perform(get("/api/v1/productos").header("Authorization", "Bearer " + vencido))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("El token ha expirado. Inicie sesión nuevamente"));
        mvc.perform(get("/api/v1/productos").header("Authorization", bearer("vendedor") + "alterado"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("El token no es válido"));
        String valido = bearer("vendedor");
        var u = usuarios.findByUsernameIgnoreCase("vendedor").orElseThrow(); u.setEstado(false); usuarios.saveAndFlush(u);
        mvc.perform(get("/api/v1/productos").header("Authorization", valido)).andExpect(status().isUnauthorized());
    }

    @Test
    void preflightCorsNoExigeJwt() throws Exception {
        mvc.perform(options("/api/v1/productos").header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
    }

    @Test
    void hashesDeLaMigracionCorrespondenALasCredenciales() throws Exception {
        String sql;
        try (var stream = getClass().getResourceAsStream("/db/migration/V2__seguridad.sql")) {
            sql = new String(Objects.requireNonNull(stream).readAllBytes(), StandardCharsets.UTF_8);
        }
        var matcher = Pattern.compile("values \\('([^']+)', '(\\$2a\\$10\\$[^']+)'").matcher(sql);
        var passwords = Map.of("admin", "Admin2026*", "vendedor", "Vendedor2026*", "almacen", "Almacen2026*",
                "supervisor", "Supervisor2026*", "inactivo", "Inactivo2026*");
        int comprobados = 0;
        while (matcher.find()) {
            assertThat(new BCryptPasswordEncoder().matches(passwords.get(matcher.group(1)), matcher.group(2))).isTrue();
            comprobados++;
        }
        assertThat(comprobados).isEqualTo(5);
    }
}

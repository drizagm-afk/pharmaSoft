package com.edu.upeu.PharmaBackend;

import com.edu.upeu.PharmaBackend.config.AuditoriaConfig;
import com.edu.upeu.PharmaBackend.dto.*;
import com.edu.upeu.PharmaBackend.entity.*;
import com.edu.upeu.PharmaBackend.mapper.VentaMapper;
import com.edu.upeu.PharmaBackend.service.service.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("dev")
@SpringBootTest(properties = {
    "spring.cache.type=none", "management.health.redis.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:auditoria;MODE=Oracle;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false",
    "spring.flyway.enabled=false"})
@Transactional
class AuditoriaIntegrationTest {
    @Autowired EntityManager em;
    @Autowired CategoriaService categorias;
    @Autowired ProductoService productos;
    @Autowired ClienteService clientes;
    @Autowired AuditoriaConfig config;

    @AfterEach
    void limpiar() { SecurityContextHolder.clearContext(); }

    @Test
    void respuestasDeCreacionYEdicionIncluyenAutoresYConservanCreador() {
        autenticar("rosa.mendoza");
        var c = categorias.create(new CategoriaRequestDTO("Cuidado respiratorio", null, true));
        var p = productos.create(new ProductoRequestDTO("Solución salina 100 ml", null,
                new BigDecimal("2.50"), 20, c.getId(), true));
        var cl = clientes.create(cliente("Lucía"));
        assertThat(c.getCreadoPor()).isEqualTo("rosa.mendoza");
        assertThat(p.getCreadoPor()).isEqualTo("rosa.mendoza");
        assertThat(cl.getCreadoPor()).isEqualTo("rosa.mendoza");
        autenticar("lucia.ramos");
        var ce = categorias.update(c.getId(), new CategoriaRequestDTO("Higiene respiratoria", null, true));
        var pe = productos.update(p.getId(), new ProductoRequestDTO("Solución salina 100 ml", null,
                new BigDecimal("2.80"), 20, c.getId(), true));
        var cle = clientes.update(cl.getId(), cliente("Lucía Elena"));
        assertThat(ce.getModificadoPor()).isEqualTo("lucia.ramos");
        assertThat(pe.getModificadoPor()).isEqualTo("lucia.ramos");
        assertThat(cle.getModificadoPor()).isEqualTo("lucia.ramos");
        assertThat(pe.getCreadoPor()).isEqualTo("rosa.mendoza");
        assertThat(pe.getFechaCreacion()).isEqualTo(p.getFechaCreacion());
        assertThat(pe.getFechaModificacion()).isNotNull();
        em.clear();
        assertThat(em.find(Categoria.class, c.getId()).getCreadoPor()).isEqualTo("rosa.mendoza");
        assertThat(em.find(Producto.class, p.getId()).getModificadoPor()).isEqualTo("lucia.ramos");
        assertThat(em.find(Cliente.class, cl.getId()).getModificadoPor()).isEqualTo("lucia.ramos");
    }

    @Test
    void ventaConservaCreadorTrasAnulacionPorOtroUsuario() {
        autenticar("lucia.ramos");
        var cl = clientes.create(cliente("Lucía"));
        Venta venta = new Venta();
        venta.setCliente(em.find(Cliente.class, cl.getId()));
        venta.setTotal(new BigDecimal("2.50"));
        em.persist(venta); em.flush();
        Long id = venta.getId();
        autenticar("rosa.mendoza");
        venta.setEstado(com.edu.upeu.PharmaBackend.enums.EstadoVenta.ANULADA);
        em.flush(); em.clear();
        assertThat(VentaMapper.ConvertToResponse(em.find(Venta.class, id)).getCreadoPor())
                .isEqualTo("lucia.ramos");
    }

    @Test
    void usaSistemaParaSesionAusenteAnonimaONoAutenticada() {
        assertThat(config.auditor().getCurrentAuditor()).contains("sistema");
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "anonymous", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        assertThat(config.auditor().getCurrentAuditor()).contains("sistema");
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.unauthenticated("rosa.mendoza", null));
        assertThat(config.auditor().getCurrentAuditor()).contains("sistema");
        Categoria c = new Categoria(); c.setNombre("Higiene personal");
        em.persist(c); em.flush(); em.clear();
        assertThat(em.find(Categoria.class, c.getId()).getCreadoPor()).isEqualTo("sistema");
    }

    private ClienteRequestDTO cliente(String nombres) {
        return new ClienteRequestDTO("47812365", nombres, "Ramos Cutipa",
                "lucia.ramos@example.com", null, null, true);
    }

    private void autenticar(String usuario) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(usuario, null, List.of()));
    }
}

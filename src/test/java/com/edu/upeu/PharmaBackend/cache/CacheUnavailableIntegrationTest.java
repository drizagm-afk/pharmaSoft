package com.edu.upeu.PharmaBackend.cache;

import com.edu.upeu.PharmaBackend.dto.CategoriaRequestDTO;
import com.edu.upeu.PharmaBackend.service.service.CategoriaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.*;

/** Uses H2 and an unreachable Redis port: no live service is stopped. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:cache-unavailable;MODE=Oracle;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false",
        "spring.flyway.enabled=false", "spring.data.redis.host=127.0.0.1", "spring.data.redis.port=1",
        "spring.data.redis.timeout=100ms", "spring.data.redis.connect-timeout=100ms"})
class CacheUnavailableIntegrationTest {
    @Autowired CategoriaService categorias;

    @Test
    void baseDeDatosSigueDisponibleCuandoFallaRedis() {
        var creada = categorias.create(new CategoriaRequestDTO("Fallback", null, true));
        assertThat(categorias.readAll()).anyMatch(c -> c.getId().equals(creada.getId()));
        categorias.update(creada.getId(), new CategoriaRequestDTO("Fallback actualizado", null, true));
        assertThat(categorias.read(creada.getId()).getNombre()).isEqualTo("Fallback actualizado");
        categorias.delete(creada.getId());
        assertThat(categorias.readAll()).isEmpty();
    }
}

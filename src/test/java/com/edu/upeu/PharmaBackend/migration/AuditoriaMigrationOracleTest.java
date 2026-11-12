package com.edu.upeu.PharmaBackend.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Opt-in: aplica y valida V4 en el Oracle de desarrollo, sin arrancar la aplicación. */
@EnabledIfSystemProperty(named = "pharma.integration", matches = "true")
class AuditoriaMigrationOracleTest {

    @Test
    void migraAuditoriaSinPerderDatosYUnaSegundaEjecucionNoRepiteDDL() throws Exception {
        var config = new YamlPropertySourceLoader()
                .load("oracleDev", new ClassPathResource("application-dev.yaml")).getFirst();
        var url = (String) config.getProperty("spring.datasource.url");
        var usuario = (String) config.getProperty("spring.datasource.username");
        var clave = String.valueOf(config.getProperty("spring.datasource.password"));

        try (var db = DriverManager.getConnection(url, usuario, clave)) {
            long productosAntes = numero(db, "select count(*) from productos");
            long ventasAntes = numero(db, "select count(*) from ventas");
            var flyway = Flyway.configure().dataSource(url, usuario, clave)
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true).baselineVersion("1").load();
            flyway.validate();
            flyway.migrate();

            assertThat(numero(db, "select count(*) from productos")).isEqualTo(productosAntes);
            assertThat(numero(db, "select count(*) from ventas")).isEqualTo(ventasAntes);
            assertThat(numero(db, """
                    select count(*) from "flyway_schema_history"
                    where "version" = '4' and "success" = 1
                    """)).isEqualTo(1);

            for (String tabla : List.of("CATEGORIAS", "PRODUCTOS", "CLIENTES")) {
                assertThat(columnasAuditoria(db, tabla))
                        .containsExactlyInAnyOrder("CREADO_POR", "MODIFICADO_POR");
            }
            assertThat(columnasAuditoria(db, "VENTAS")).containsExactly("CREADO_POR");
            assertThat(numero(db, """
                    select count(*) from user_tab_columns
                    where table_name in ('CATEGORIAS','PRODUCTOS','CLIENTES','VENTAS')
                      and column_name in ('CREADO_POR','MODIFICADO_POR')
                      and data_type = 'VARCHAR2' and char_length = 50
                      and char_used = 'C' and nullable = 'Y'
                    """)).isEqualTo(7);

            assertThat(textos(db, """
                    select column_name from user_tab_columns
                    where table_name = 'AUDITORIA_EVENTOS' order by column_id
                    """, null)).containsExactly("ID", "FECHA", "USUARIO", "ACCION", "ENTIDAD",
                    "ENTIDAD_ID", "DETALLE", "REQUEST_ID");
            assertThat(numero(db, """
                    select count(*) from user_tab_columns
                    where table_name = 'AUDITORIA_EVENTOS' and column_name = 'ID'
                      and identity_column = 'YES'
                    """)).isEqualTo(1);
            assertThat(numero(db, """
                    select count(*) from user_constraints
                    where table_name = 'AUDITORIA_EVENTOS'
                      and constraint_name = 'PK_AUDITORIA_EVENTOS' and constraint_type = 'P'
                    """)).isEqualTo(1);
            comprobarIndice(db, "IX_AUDITORIA_FECHA", "FECHA");
            comprobarIndice(db, "IX_AUDITORIA_USUARIO", "USUARIO", "FECHA");
            comprobarIndice(db, "IX_PRODUCTOS_NOMBRE", "NOMBRE");

            flyway.validate();
            assertThat(flyway.migrate().migrationsExecuted).isZero();
        }
    }

    private List<String> columnasAuditoria(Connection db, String tabla) throws Exception {
        return textos(db, """
                select column_name from user_tab_columns
                where table_name = ? and column_name in ('CREADO_POR','MODIFICADO_POR')
                order by column_id
                """, tabla);
    }

    private void comprobarIndice(Connection db, String nombre, String... columnas) throws Exception {
        assertThat(textos(db, """
                select column_name from user_ind_columns
                where index_name = ? order by column_position
                """, nombre)).containsExactly(columnas);
        assertThat(textos(db, "select status from user_indexes where index_name = ?", nombre))
                .containsExactly("VALID");
    }

    private List<String> textos(Connection db, String sql, String parametro) throws Exception {
        try (var sentencia = db.prepareStatement(sql)) {
            if (parametro != null) sentencia.setString(1, parametro);
            try (var resultado = sentencia.executeQuery()) {
                var valores = new ArrayList<String>();
                while (resultado.next()) valores.add(resultado.getString(1));
                return valores;
            }
        }
    }

    private long numero(Connection db, String sql) throws Exception {
        try (var sentencia = db.createStatement(); var resultado = sentencia.executeQuery(sql)) {
            resultado.next();
            return resultado.getLong(1);
        }
    }
}

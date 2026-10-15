package com.edu.upeu.PharmaBackend.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Opt-in: applies Flyway migrations to the configured development Oracle schema. */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfSystemProperty(named="pharma.integration", matches="true")
class OracleSecurityIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @Test
    void migracionYLoginReales() throws Exception {
        assertThat(jdbc.queryForObject("select count(*) from \"flyway_schema_history\" where \"version\" = '2' and \"success\" = 1", Integer.class))
                .isEqualTo(1);
        mvc.perform(get("/api/health")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/productos")).andExpect(status().isUnauthorized());
        for (String[] cuenta : new String[][]{
                {"admin", "Admin2026*"}, {"vendedor", "Vendedor2026*"},
                {"almacen", "Almacen2026*"}, {"supervisor", "Supervisor2026*"}}) {
            mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"" + cuenta[0] + "\",\"password\":\"" + cuenta[1] + "\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.token").isString());
        }
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"inactivo\",\"password\":\"Inactivo2026*\"}"))
                .andExpect(status().isUnauthorized());
    }
}

package com.edu.upeu.PharmaBackend.controller;

import com.edu.upeu.PharmaBackend.config.JsonValidationConfig;
import com.edu.upeu.PharmaBackend.dto.ProductoRequestDTO;
import com.edu.upeu.PharmaBackend.exception.GlobalExceptionHandler;
import com.edu.upeu.PharmaBackend.exception.ReglaNegocioException;
import com.edu.upeu.PharmaBackend.service.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({ProductoController.class, CategoriaController.class})
@Import({JsonValidationConfig.class, GlobalExceptionHandler.class})
class DependenciasHttpTest {
    @Autowired MockMvc mvc;
    @MockitoBean ProductoService productos;
    @MockitoBean CategoriaService categorias;
    private String body(String stock) {
        return "{\"nombre\":\"Producto QA\",\"precio\":5.50,\"stock\":" + stock + ",\"estado\":true,\"categoriaId\":46}";
    }
    @Test void fractionalStockPostIs400AndNeverCallsService() throws Exception {
        mvc.perform(post("/api/v1/productos").contentType(MediaType.APPLICATION_JSON).content(body("1.5")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").exists()); verifyNoInteractions(productos);
    }
    @Test void fractionalStockPutIs400AndNeverCallsService() throws Exception {
        mvc.perform(put("/api/v1/productos/87").contentType(MediaType.APPLICATION_JSON).content(body("1.5")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.path").value("/api/v1/productos/87"));
        verifyNoInteractions(productos);
    }
    @ParameterizedTest @ValueSource(ints={0,1,10})
    void integerStockStillReachesCreate(int stock) throws Exception {
        mvc.perform(post("/api/v1/productos").contentType(MediaType.APPLICATION_JSON).content(body(String.valueOf(stock))))
            .andExpect(status().isCreated()); verify(productos).create(argThat(p -> p.getStock()==stock));
    }
    @Test void negativeStockIsValidation400() throws Exception {
        mvc.perform(post("/api/v1/productos").contentType(MediaType.APPLICATION_JSON).content(body("-1")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.validationErrors.stock").exists());
        verifyNoInteractions(productos);
    }
    @Test void malformedJsonIsStructured400() throws Exception {
        mvc.perform(post("/api/v1/productos").contentType(MediaType.APPLICATION_JSON).content("{"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("Bad Request")); verifyNoInteractions(productos);
    }
    @Test void validCategoryUpdateReturns200() throws Exception {
        mvc.perform(put("/api/v1/categorias/46").contentType(MediaType.APPLICATION_JSON)
            .content("{\"nombre\":\"Categoria QA\",\"estado\":true}"))
            .andExpect(status().isOk()); verify(categorias).update(eq(46L), any());
    }
    @Test void businessRejectionIs409WithMessage() throws Exception {
        when(productos.create(any())).thenThrow(new ReglaNegocioException("Categoria inactiva"));
        mvc.perform(post("/api/v1/productos").contentType(MediaType.APPLICATION_JSON).content(body("10")))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("Categoria inactiva"));
    }
}

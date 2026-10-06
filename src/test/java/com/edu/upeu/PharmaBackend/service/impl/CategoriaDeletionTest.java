package com.edu.upeu.PharmaBackend.service.impl;
import com.edu.upeu.PharmaBackend.controller.CategoriaController;
import com.edu.upeu.PharmaBackend.exception.GlobalExceptionHandler;
import com.edu.upeu.PharmaBackend.repository.CategoriaRepository;
import com.edu.upeu.PharmaBackend.repository.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class CategoriaDeletionTest {
 private final CategoriaRepository categorias=mock(CategoriaRepository.class);
 private final ProductoRepository productos=mock(ProductoRepository.class);
 private final CategoriaServiceImpl service=new CategoriaServiceImpl(categorias,productos);
 private org.springframework.test.web.servlet.MockMvc mvc() { return MockMvcBuilders.standaloneSetup(new CategoriaController(service)).setControllerAdvice(new GlobalExceptionHandler()).build(); }
 @Test void referencedCategoryReturns409AndIsPreserved() throws Exception {
  when(categorias.existsById(47L)).thenReturn(true); when(productos.existsByCategoriaId(47L)).thenReturn(true);
  mvc().perform(delete("/api/v1/categorias/47")).andExpect(status().isConflict()).andExpect(jsonPath("status").value(409)).andExpect(jsonPath("message").value("No se puede eliminar la categoría porque tiene productos asociados. Reasigne esos productos a otra categoría antes de intentarlo de nuevo."));
  verify(categorias,never()).deleteById(anyLong());
 }
 @Test void emptyCategoryCanBeDeleted() throws Exception {
  when(categorias.existsById(47L)).thenReturn(true); mvc().perform(delete("/api/v1/categorias/47")).andExpect(status().isNoContent()); verify(categorias).deleteById(47L);
 }
 @Test void missingCategoryReturns404() throws Exception {
  mvc().perform(delete("/api/v1/categorias/999")).andExpect(status().isNotFound()); verifyNoInteractions(productos); verify(categorias,never()).deleteById(anyLong());
 }
}
package com.edu.upeu.PharmaBackend.service.impl;

import com.edu.upeu.PharmaBackend.dto.*;
import com.edu.upeu.PharmaBackend.entity.*;
import com.edu.upeu.PharmaBackend.exception.*;
import com.edu.upeu.PharmaBackend.repository.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DependenciasRegressionTest {
    private final ProductoRepository productos = mock(ProductoRepository.class);
    private final CategoriaRepository categorias = mock(CategoriaRepository.class);
    private final ProductoServiceImpl productoService = new ProductoServiceImpl(productos, categorias);
    private final CategoriaServiceImpl categoriaService = new CategoriaServiceImpl(categorias, productos);

    private Categoria categoria(long id, boolean activa) {
        var c = new Categoria(); c.setId(id); c.setNombre("Categoria QA"); c.setEstado(activa); return c;
    }
    private ProductoRequestDTO solicitud(long categoriaId, String nombre) {
        return new ProductoRequestDTO(nombre, null, new BigDecimal("5.50"), 10, categoriaId, true);
    }
    private Producto existente(Categoria categoria) {
        var p = new Producto(); p.setId(87L); p.setNombre("Original QA"); p.setPrecio(new BigDecimal("5.50"));
        p.setStock(10); p.setEstado(true); p.setCategoria(categoria); return p;
    }
    @Test void rejectsCreateInInactiveCategoryWithoutSaving() {
        when(categorias.findByIdForUpdate(49L)).thenReturn(Optional.of(categoria(49L, false)));
        assertThrows(ReglaNegocioException.class, () -> productoService.create(solicitud(49L, "Nuevo QA")));
        verify(productos, never()).save(any());
    }
    @Test void rejectsMoveToInactiveCategoryWithoutMutatingProduct() {
        var original = categoria(46L, true); var p = existente(original);
        when(categorias.findByIdForUpdate(49L)).thenReturn(Optional.of(categoria(49L, false)));
        when(productos.findById(87L)).thenReturn(Optional.of(p));
        assertThrows(ReglaNegocioException.class, () -> productoService.update(87L, solicitud(49L, "Nuevo nombre")));
        assertSame(original, p.getCategoria()); assertEquals("Original QA", p.getNombre());
        verify(productos, never()).save(any());
    }
    @Test void missingCategoryDoesNotSave() {
        when(categorias.findByIdForUpdate(999L)).thenReturn(Optional.empty());
        assertThrows(RecursosNoEncontradosException.class, () -> productoService.update(87L, solicitud(999L, "Nuevo QA")));
        verify(productos, never()).save(any());
    }
    @Test void duplicateUpdateUsesTrimmedNameAndExcludesCurrentId() {
        var c = categoria(46L, true); var p = existente(c);
        when(categorias.findByIdForUpdate(46L)).thenReturn(Optional.of(c));
        when(productos.findById(87L)).thenReturn(Optional.of(p));
        when(productos.existsByNombreIgnoreCaseAndIdNot("otro qa", 87L)).thenReturn(true);
        assertThrows(ReglaNegocioException.class, () -> productoService.update(87L, solicitud(46L, " otro qa ")));
        assertEquals("Original QA", p.getNombre()); verify(productos, never()).save(any());
    }
    @Test void validUpdateMayKeepOwnNameAndMoveToActiveCategory() {
        var destination = categoria(47L, true); var p = existente(categoria(46L, true));
        when(categorias.findByIdForUpdate(47L)).thenReturn(Optional.of(destination));
        when(productos.findById(87L)).thenReturn(Optional.of(p));
        var response = productoService.update(87L, solicitud(47L, " Original QA "));
        assertSame(destination, p.getCategoria()); assertEquals("Original QA", response.getNombre());
        verify(productos).existsByNombreIgnoreCaseAndIdNot("Original QA", 87L); verify(productos).save(p);
    }
    @Test void validCreateInActiveCategorySaves() {
        var c = categoria(46L, true); when(categorias.findByIdForUpdate(46L)).thenReturn(Optional.of(c));
        var response = productoService.create(solicitud(46L, " Nuevo QA "));
        assertEquals("Nuevo QA", response.getNombre()); assertEquals(46L, response.getCategoriaId()); verify(productos).save(any());
    }
    @Test void refusesDeactivationBeforeChangingCategory() {
        var c = categoria(46L, true); when(categorias.findByIdForUpdate(46L)).thenReturn(Optional.of(c));
        when(productos.existsByCategoriaIdAndEstadoTrue(46L)).thenReturn(true);
        assertThrows(ReglaNegocioException.class, () -> categoriaService.update(46L, new CategoriaRequestDTO("Nuevo nombre", null, false)));
        assertTrue(c.getEstado()); assertEquals("Categoria QA", c.getNombre()); verify(categorias, never()).save(any());
    }
    @Test void permitsDeactivationWithoutActiveProducts() {
        var c = categoria(46L, true); when(categorias.findByIdForUpdate(46L)).thenReturn(Optional.of(c));
        categoriaService.update(46L, new CategoriaRequestDTO("Categoria QA", null, false));
        assertFalse(c.getEstado()); verify(categorias).save(c);
    }
    @Test void permitsActiveCategoryEditWithActiveProducts() {
        var c = categoria(46L, true); when(categorias.findByIdForUpdate(46L)).thenReturn(Optional.of(c));
        categoriaService.update(46L, new CategoriaRequestDTO("Categoria editada", null, true));
        assertTrue(c.getEstado()); assertEquals("Categoria editada", c.getNombre()); verify(categorias).save(c);
    }
    @Test void categoryDeletionStillProtectsInactiveReferences() {
        when(categorias.existsById(66L)).thenReturn(true); when(productos.existsByCategoriaId(66L)).thenReturn(true);
        assertThrows(ReglaNegocioException.class, () -> categoriaService.delete(66L)); verify(categorias, never()).deleteById(anyLong());
    }
}

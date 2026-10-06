package com.edu.upeu.PharmaBackend.service.impl;
import com.edu.upeu.PharmaBackend.entity.Producto;
import com.edu.upeu.PharmaBackend.repository.ProductoRepository;
import com.edu.upeu.PharmaBackend.repository.CategoriaRepository;
import com.edu.upeu.PharmaBackend.exception.ReglaNegocioException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class ProductoContractTest {
 private final ProductoRepository repo=mock(ProductoRepository.class);
 private final ProductoServiceImpl service=new ProductoServiceImpl(repo,mock(CategoriaRepository.class));
 @Test void paginationPreservesMetadataAndStableSorting() {
  var request=PageRequest.of(1,5,Sort.by(Sort.Direction.DESC,"precio").and(Sort.by("id")));
  when(repo.findAll(request)).thenReturn(new PageImpl<>(java.util.List.of(),request,8));
  var page=service.listar(1,5,"precio","desc"); assertEquals(1,page.pagina()); assertEquals(5,page.tamanio()); assertEquals(8,page.totalElementos()); assertEquals(2,page.totalPaginas()); assertTrue(page.ultima()); verify(repo).findAll(request);
 }
 @Test void rejectsInvalidQueriesWithoutRepositoryAccess() {
  assertThrows(ReglaNegocioException.class,()->service.listar(-1,5,"nombre","asc")); assertThrows(ReglaNegocioException.class,()->service.listar(0,101,"nombre","asc")); assertThrows(ReglaNegocioException.class,()->service.listar(0,5,"categoriaId","asc")); assertThrows(ReglaNegocioException.class,()->service.listar(0,5,"nombre","wrong")); verifyNoInteractions(repo);
 }
 @Test void deletionRetainsProductAndMarksInactive() {
  var p=new Producto(); p.setEstado(true); when(repo.findById(42L)).thenReturn(Optional.of(p)); service.delete(42L); assertFalse(p.getEstado()); verify(repo).save(p); verify(repo,never()).deleteById(anyLong());
 }
 @Test void repeatedDeletionIsBusinessConflict() {
  var p=new Producto(); p.setEstado(false); when(repo.findById(42L)).thenReturn(Optional.of(p)); assertThrows(ReglaNegocioException.class,()->service.delete(42L)); verify(repo,never()).save(any());
 }
}
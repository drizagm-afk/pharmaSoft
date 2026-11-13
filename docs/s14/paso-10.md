# S14 - Paso 10: corrección de búsqueda completa (INC-01)

Fecha: 13 de noviembre de 2026.

## Problema y cambio

Nueva venta filtraba únicamente los primeros 100 registros cargados. Se agregan dos consultas del servidor que aplican los filtros antes de paginar:

```http
GET /api/v1/productos/buscar?texto=Vitamina%20D&pagina=0&tamanio=6
GET /api/v1/clientes/buscar?texto=huaman&pagina=0&tamanio=6
```

Los productos se buscan por nombre o categoría y se devuelven solo cuando están activos y tienen stock positivo. Los clientes se buscan por nombres, apellidos o DNI y deben estar activos. La búsqueda permite ambos órdenes de nombres y apellidos.

La normalización del término y la traducción de caracteres en SQL permiten buscar sin distinguir mayúsculas ni las tildes españolas. LOCATE trata el término como texto literal; un guion bajo no actúa como comodín. Un término vacío devuelve una página vacía sin consultar todo el catálogo.

Las respuestas usan PaginaResponse, con tamaño predeterminado 6, página desde cero y validación de tamaño entre 1 y 100. El orden incluye ID como desempate. Estas búsquedas no se almacenan en caché para consultar disponibilidad actual. Los listados existentes, su contrato y su caché conservan el comportamiento anterior.

Las reglas GET de seguridad existentes para productos y clientes protegen las rutas nuevas. No se agregan permisos ni migraciones.

## Verificación

- Antes de implementar las rutas, BusquedaVentaIntegrationTest falló en los dos casos, con HTTP 400 en las URL de búsqueda inexistentes.
- Después de la corrección, ambas pruebas pasan. Los datos aislados incluyen más de 100 productos y clientes; se verifica que los registros buscados no estén en su primera página anterior.
- Se comprueban disponibilidad, categoría, nombres completos, DNI, tildes, término vacío y caracteres literales.
- Suite general: 104 pruebas descubiertas; 86 aprobadas, 18 opcionales omitidas, sin fallos ni errores.
- Ejecución final de búsqueda y Redis: 9 pruebas aprobadas, sin fallos, errores ni omisiones. Confirma la serialización y la invalidación de caché anteriores.
- Oracle real: Vitamina D, ID 133, está fuera de los primeros 100 productos y aparece en la búsqueda nueva. Buscar huaman devuelve el cliente con apellido Huamán.
- El frontend supera las tres repeticiones de los siete E2E, incluyendo la búsqueda de Vitamina D antes del registro de cada venta.

Los detalles de la SPA, las tres ventas de estabilidad y las evidencias están en docs/s14/10-estabilizacion.md del frontend.

Logs locales del backend: target/s14-inc01-antes.log, target/s14-inc01-backend.log, target/s14-paso10-tests.log y target/s14-paso10-cache-tests.log.

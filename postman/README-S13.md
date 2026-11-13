# Postman — Sesión 13

Importar PharmaSoft-S13-Optimizacion.postman_collection.json desde Import → Files.
Incluye 35 peticiones agrupadas por prueba y variables de colección; no necesita un
archivo de entorno separado. No seleccionar un entorno antiguo que sobrescriba esas
variables. Las contraseñas iniciales son las cuentas demo existentes; editarlas si cambiaron.

## Uso

1. Abrir la colección → Variables. Mantener backendUrl=http://localhost:8080 y
   webUrl=http://localhost:8081. Ejecutar primero las dos peticiones de salud de 00.
   Un 502 por Nginx con salud directa fallida exige arrancar/corregir el backend.
2. Ejecutar Login admin y Login vendedor. Sus scripts guardan tokenAdmin y tokenVendedor
   automáticamente. Repetir después de reiniciar Java o cambiar la clave JWT.
3. Ejecutar por carpetas según sus descripciones. No usar Run collection completo:
   las pruebas de caída/recuperación de Redis requieren estados opuestos, y la prueba
   de SQL se hace en dev antes de regresar a prod.
4. P08 crea una categoría y guarda su nombre/ID. No repetir el POST si ya se obtuvo 201.
5. Para P09 completar clienteId, productoId y cantidad. Consultar primero clientes y
   productos. Elegir producto activo con stock suficiente. La primera lista guarda
   stockAntes y llena la caché paginada; el POST registra una venta real y guarda ventaId;
   la última lista verifica el descuento. Si el producto no está en las primeras 100
   filas, ajustar la misma paginación en ambos listados para incluirlo.
6. Si la venta se registra en la SPA, omitir el POST de venta de Postman y conservar la
   medición previa de stock. No registrar otra venta solo para duplicar evidencia.
7. Para los encabezados estáticos, completar mainAsset y stylesAsset con los nombres
   con hash que aparecen en el build actual, sin barra inicial.

Las variables categoriasTiempo1Ms, categoriasTiempo2Ms, ultimoRequestId y
cacheHitsCategorias conservan resultados útiles para el informe. Son resultados
del proceso actual, no sustituyen capturas del log ni mediciones históricas.

## Evidencias externas

- P01: salida del build y lazy chunks.
- P02/P03: permisos y transiciones de @defer en el navegador.
- P04/P05: también capturar encabezados en DevTools.
- P06: primera visita y F5 con Disable cache desmarcado; Postman no mide la caché del navegador.
- P07: SQL/log del servicio y comandos Redis KEYS/TTL.
- P10: detener/restaurar Redis y capturar WARN del backend.
- P11: correlacionar el ID del encabezado con la línea del archivo de log.
- P14: búsqueda de secretos, perfil prod, logs y configuración de rotación.

Consultar la guía completa en el frontend: docs/s13-practica/guia-validacion-manual.md.
Los tests HTTP comprueban estado/cuerpo/encabezados; no demuestran por sí solos
ausencia de SQL, invalidación interna, ejecución de Angular ni configuración del proceso.

## Revisión del archivo

Se comprobó que el JSON se puede leer, que los 41 scripts JavaScript tienen sintaxis
válida y que los tokens exportados están vacíos. No se ejecutaron peticiones contra
la base de datos: las acciones y capturas manuales quedan a cargo de la validación.
Antes de exportar una colección ya utilizada, vaciar tokens y contraseñas personales.

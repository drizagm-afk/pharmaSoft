# Etapa 4 — Preparación de las mediciones de caché y monitoreo

## Alcance y estado

Preparación de las partes C y D de la actividad autónoma. La colección contiene
26 peticiones y no ejecuta escrituras de negocio: las categorías, cambios de precio
y ventas se realizan una vez desde la SPA para conservar evidencia de interacción.
No se ha ejecutado la matriz ni se han medido tiempos reales en esta etapa.

El código actual ya invalida categorías al crear/eliminar; ambas cachés al renombrar
categorías; productos al escribir productos o registrar ventas. No se modifican estas
anotaciones sin observar un fallo. Si aparece uno, conservar su resultado inicial,
corregirlo y repetir exactamente el caso afectado para documentar antes/después.

## Importar y configurar

Importar postman/PharmaSoft-S13-Autonoma.postman_collection.json. Usar sus variables
de colección, sin un entorno antiguo que sobrescriba sus valores.

- backendUrl: http://localhost:8080.
- adminUsername: admin; completar adminPassword localmente con la clave vigente.
- tokenAdmin se guarda automáticamente desde el login; repetirlo después de reiniciar Java.
- categoriaId: categoría creada para C-01 o categoría vacía que se eliminará en C-05.
- productoId: producto activo seleccionado para C-02, C-03 y C-04.
- categoriaNombreNuevo y precioNuevo: resultados esperados, introducidos antes de comprobar.
- stockAntesVenta y cantidadVendida: referencia previa y cantidad vendida; no cambiarlos
  para hacer coincidir un resultado incorrecto.
- ventaId y requestIdVenta: anotar los de la venta registrada desde la SPA.

NO ejecutar la colección completa. Solo la carpeta 02 se corre con 20 iteraciones;
las demás se ejecutan una vez en su momento. No exportar una copia con contraseña o token.

## C — Veinte lecturas por catálogo, aciertos y tiempos

1. Arrancar Oracle y Redis. Reiniciar el backend con prod. Cerrar las pestañas de la SPA
   durante el experimento para que no añadan lecturas a los contadores.
2. Ejecutar el login admin de la carpeta 00.
3. Desde el directorio del FRONTEND, eliminar únicamente las dos entradas del experimento:

   ```text
   docker compose -f deploy/docker-compose.yml exec redis redis-cli DEL categorias::todas productos::0-10-nombre-asc
   ```

4. Ejecutar una vez la carpeta 01: snapshot ANTES de hits y misses de ambas cachés.
   Si una métrica todavía no está registrada, realizar una lectura de cada catálogo,
   repetir el DEL anterior y obtener todos los snapshots ANTES otra vez. Esas lecturas
   preparatorias quedan fuera del experimento por la resta de contadores.
5. En Runner seleccionar SOLO la carpeta 02, Iterations=20 y Delay=0. No ejecutar
   peticiones adicionales de catálogo. Se realizan veinte GET /categorias y veinte
   GET /productos?pagina=0, con tamaño/orden por defecto.
6. Capturar el resumen del Runner con las cuarenta respuestas correctas y sus tests.
   Si alguna petición falla, conservar el fallo y repetir una serie completa tras
   restablecer condiciones; no mezclar muestras de ejecuciones diferentes.
7. Ejecutar una vez la carpeta 03: snapshot DESPUÉS. Capturar los COUNT de las cuatro
   métricas, cada URL filtrada por name y result, y los resultados de sus tests.
8. Las variables resumen_categorias_cache y resumen_productos_cache contienen
   hits, misses, total y tasaAciertosPorcentaje. Se calculan por diferencias:

   ```text
   hits = COUNT_hit_después − COUNT_hit_antes
   misses = COUNT_miss_después − COUNT_miss_antes
   tasa = 100 × hits / (hits + misses)
   ```

   Los scripts comprueban veinte lecturas por caché, sin imponer un 95 % ficticio.
   Una serie fría ideal de 1 miss y 19 hits produce 95 %, pero el informe usa lo medido.
9. tiempos_categorias y tiempos_productos guardan las veinte duraciones HTTP.
   resumen_*_tiempos guarda primerGet y mediaSiguientes19, en milisegundos.
   Capturar las variables de resultado sin mostrar credenciales ni token.
10. Comparar el primer GET con la media de los siguientes. Identificarlo como miss/hits
    solo si la preparación fría y los contadores respaldan esa interpretación.
    No equiparar responseTime de Postman con duracionMs interno del servicio.

## C — Vigencia de un minuto y restauración a diez

No es necesario editar application.yaml ni hacer un commit con el TTL experimental.
Para evitar ambigüedades de nombres de variables, usar un argumento de arranque:

1. Detener Java y añadir en Program arguments del IDE:

   ```text
   --spring.cache.redis.time-to-live=1m
   ```

   Mantener las variables de prod. Reiniciar, renovar token y borrar categorias::todas.
2. Ejecutar una sola vez la lectura de la carpeta 04 para crear la entrada.
3. Desde el frontend:

   ```text
   docker compose -f deploy/docker-compose.yml exec redis redis-cli TTL categorias::todas
   ```

   Capturar un TTL positivo no mayor de 60 segundos. No volver a listar categorías
   durante la espera; mantener la SPA cerrada para evitar recrear la entrada.
4. Esperar algo más de un minuto y repetir SOLO el comando TTL. Capturar -2:
   la entrada expiró. -1 significa que existe sin expiración y no es el resultado esperado.
5. Detener Java, quitar el argumento temporal y arrancar con el valor normal de 10m.
6. Renovar token, eliminar la misma clave, ejecutar una lectura y capturar su TTL
   positivo próximo a 600 segundos. Las entradas antiguas no cambian de TTL solo
   por reiniciar Java: por eso se recrea la entrada en ambas fases.

## C — Matriz de invalidación y capturas

Usar un producto de prueba activo y un cliente activo. Anotar los nombres/precio
originales antes de modificarlos. No borrar una categoría que tenga productos.
Antes de C-02, C-03 y C-04, ejecutar tanto el detalle de producto como el listado
de calentamiento de la carpeta 05. Así se verifica una entrada previamente cacheada.
El producto debe figurar en la página consultada; la colección lo comprueba.
Si no figura entre los primeros 100, ajustar la misma página en todas las consultas.

| Caso | Acción manual y evidencia anterior | Captura posterior y verificación |
|---|---|---|
| C-01 | Abrir Categorías para llenar caché; crear una categoría de prueba. | Categoría visible inmediatamente; guardar su ID y comprobar C-01 en Postman. |
| C-02 | Elegir categoría que tenga el producto seleccionado; guardar nombre original y calentar detalle/lista. Renombrarla en SPA. | Columna Categoría muestra el nombre nuevo; categoriaNombreNuevo debe coincidir. Capturar también el test C-02. |
| C-03 | Guardar precio original, calentar detalle/lista y cambiar solo precio desde SPA. | Capturar precioNuevo tanto en GET /productos/{id} como en el listado de Postman/SPA, sin esperar TTL. |
| C-04 | Guardar stock previo, calentar detalle/lista y registrar UNA venta desde SPA. | Capturar comprobante/número y stock descontado en lista; test C-04 usa stockAntesVenta − cantidadVendida. |
| C-05 | Crear o elegir categoría vacía; anotar ID y listar antes de eliminar para llenar caché. | Eliminar desde SPA y capturar lista inmediata sin ella; comprobar C-05 con ese ID. |
| C-06 | Detener Redis y abrir Productos desde la SPA. | Capturar listado funcional, respuesta correcta, WARN y health DOWN; realizar las dos consultas de salud con Redis apagado. |

C-04 se reutiliza para la trazabilidad de venta de Parte D; no registrar otra venta
solo para repetir evidencia. Restaurar nombre/precio originales después de capturar
los resultados y comprobar su restauración. No restaurar stock mediante un PUT que
oculte la venta: conservar el efecto real y su número.

Para C-06, desde el frontend:

```text
docker compose -f deploy/docker-compose.yml stop redis
```

Durante la caída no hacer escrituras. Ejecutar la carpeta 06: salud sin token y con
admin, ambas con 503/DOWN; la administrativa identifica Redis DOWN y Oracle UP.
Después restaurar:

```text
docker compose -f deploy/docker-compose.yml start redis
```

Esperar que Redis se recupere antes de continuar. No iniciar el contenedor independiente
de respaldo, que ocupa el mismo puerto. No detener Oracle ni Nginx.

## D — Trazabilidad de la venta y error visible

1. Durante C-04, abrir Network antes de confirmar la venta. Seleccionar el POST
   /api/v1/ventas correcto y capturar su X-Request-Id y HTTP 201. Capturar el comprobante.
2. Guardar su ID como requestIdVenta. Buscar ese valor literal en logs/pharma-backend.log
   y capturar TODAS sus coincidencias. Explicar cada línea que realmente exista;
   actualmente el registro de venta tiene como mínimo la línea de acceso del filtro
   con método, ruta, estado y duración, no mensajes inventados de pasos internos.
3. Desde SPA, abrir un producto inexistente, por ejemplo /productos/999999999/editar.
   Capturar la alerta con Código de seguimiento. Capturar el cuerpo de la respuesta
   de error y el encabezado con el mismo ID. La petición de la carpeta 08 comprueba
   además el contrato cuerpo/header, pero no sustituye el mensaje visible en SPA.
4. Explicar que soporte busca el código sin necesitar contraseña, DNI ni token.

## D — Auditoría de secretos

Usar el archivo de PRODUCCIÓN, no el log de SQL de dev. Realizar búsquedas locales,
sin publicar el contenido sensible encontrado:

- Expresión regular, sin distinguir mayúsculas: password|Bearer |secret.
- Contraseñas vigentes, valor exacto de la clave JWT y tokens de prueba: buscar cada
  valor localmente; no capturar el valor escrito en la búsqueda ni guardarlo en el informe.
- DNI completos: buscar patrones de ocho dígitos y revisar el contexto para distinguir
  DNI de fechas/otros números. No sustituir esto por afirmar que todo número es un DNI.

Capturar búsquedas genéricas y resultados sin exponer secretos. Registrar si se
encontraron coincidencias reales, qué origen se corrigió y el resultado posterior.
Si el log contiene SQL de dev con la palabra password, separar correctamente el
perfil y archivo antes de interpretar la auditoría de prod. Un nombre de columna
no es por sí solo una contraseña filtrada, aunque deba explicarse la coincidencia.

## D — Salud y métrica de ventas

La carpeta 06 proporciona las dos capturas con Redis apagado. Con Redis encendido,
ejecutar carpeta 07 para completar las otras dos:

| Redis | Sin token | Con admin |
|---|---|---|
| Encendido | 200 y status UP sin detalles | 200, componentes UP |
| Apagado | 503 y status DOWN sin detalles | 503, Redis DOWN y db UP |

Después de la venta, ejecutar en carpeta 08:

```text
/actuator/metrics/http.server.requests?tag=uri:/api/v1/ventas
```

Capturar la URL filtrada y las mediciones. COUNT cuenta solicitudes de esa URI,
no necesariamente ventas exitosas; TOTAL_TIME es acumulado y MAX es el máximo
según la ventana del medidor. Usar baseUnit para interpretar unidades y dividir
TOTAL_TIME/COUNT para una media cuando COUNT sea positivo. Si aparecen varios
métodos/estados, explicarlos o añadir esos filtros sin confundir el agregado con
la duración de una venta individual. No reiniciar Java entre la venta y la captura:
los medidores son del proceso actual.

## Resultado de esta preparación

JSON y 35 scripts comprobados sintácticamente. Simulación aislada de 20 iteraciones
por catálogo y snapshots de métricas: 60 aserciones correctas. La simulación comprueba
la aritmética con datos ficticios; no son resultados del sistema ni evidencia de mejora.
Detalle: validacion-coleccion.json. No se ejecutaron peticiones reales ni escrituras,
no se cambiaron TTL de servicios ni se detuvieron contenedores.


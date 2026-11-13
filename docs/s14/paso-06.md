# S14 — Paso 6: autores de creación y modificación

## Implementación

AuditoriaConfig habilita la auditoría JPA. El bean auditor obtiene el nombre de la autenticación del contexto de seguridad; cuando no hay una sesión autenticada o la sesión es anónima, registra sistema.

Categoría, producto y cliente incorporan creadoPor y modificadoPor. Venta incorpora creadoPor. Las cuatro entidades usan AuditingEntityListener. El creador se marca como no actualizable y los callbacks existentes conservan la gestión de fechas.

Los DTO de respuesta y sus mappers incluyen estos campos. Los DTO de entrada conservan su contrato: el autor se determina en el servidor. Las actualizaciones de categoría, producto y cliente se sincronizan con saveAndFlush antes de construir la respuesta, para devolver el editor y la fecha actuales.

Los registros históricos mantienen sus autores nulos. No se atribuyen operaciones anteriores a usuarios actuales. Spring Data también inicializa modificadoPor al crear un registro; después lo reemplaza con el usuario de cada actualización.

## Verificación

Fecha: 13 de noviembre de 2026.

| Comprobación | Resultado |
|---|---|
| Creación de categoría, producto y cliente | Respuesta con el usuario autenticado como creador. |
| Actualización por otro usuario | Nuevo editor en la respuesta inmediata y en la entidad recargada; creador conservado. |
| Fechas | Fecha de creación conservada y fecha de modificación asignada. |
| Venta anulada por otro usuario | Creador original conservado al recargar y mapear. |
| Sesión ausente, anónima o no autenticada | Auditor sistema; persistencia comprobada. |
| Redis real | Los autores sobreviven a la serialización de categoría y página de productos. La segunda lectura mantiene el número de consultas SQL. |
| Suite general | 93 pruebas descubiertas: 75 aprobadas, 18 opcionales omitidas, 0 fallos y 0 errores. |
| Auditoría y Redis, ejecución específica final | 10 aprobadas, 0 omitidas, 0 fallos y 0 errores. |
| Inicio contra Oracle | Perfil dev, servidor en 8080; health UP. |
| Contrato OpenAPI | Ambos autores en las tres respuestas de mantenimiento; solo creador en venta. |

AuditoriaIntegrationTest usa H2 aislado y rollback de cada prueba. CatalogoCacheIntegrationTest usa H2 y Redis de pruebas en la base 15 con prefijo s13-test, conservando el aislamiento existente. Estas ejecuciones no insertan datos en Oracle.

Comandos ejecutados desde la raíz del backend:

```text
./mvnw.cmd -o -Dspring.profiles.active=dev test
./mvnw.cmd -o -Dspring.profiles.active=dev -Dpharma.cache.integration=true -Dtest=AuditoriaIntegrationTest,CatalogoCacheIntegrationTest test
```

Los logs locales de ejecución están en target/s14-paso06-tests.log y target/s14-paso06-cache-tests.log. Se conservan como archivos locales de trabajo, fuera del commit.

El backend se inició con un argumento explícito de perfil dev, sin cambiar el perfil guardado en application.yaml. Su salida está en logs/s14-dev-console.log y logs/s14-dev.log.

## Siguiente etapa

El paso 7 implementará el registro de eventos y su consulta protegida. Este paso completa únicamente los autores automáticos de las entidades y las respuestas de la API.

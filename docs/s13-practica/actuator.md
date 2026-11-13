# S13: salud y métricas con Actuator

Fecha: 5 de noviembre de 2026. Tercer bloque del backend.

Se incorpora spring-boot-starter-actuator y se exponen únicamente health, info y
metrics. SecurityConfig permite salud y sus subrutas sin token; el resto de /actuator/**
requiere USUARIOS_ADMINISTRAR. Los detalles de salud se muestran exclusivamente
a ROLE_ADMIN mediante show-details=when-authorized y roles=ADMIN.

| Endpoint | Acceso y resultado |
| --- | --- |
| /actuator/health | Público: estado sin componentes ni detalles. UP produce 200; DOWN produce 503. |
| /actuator/health con admin | Incluye los indicadores de base de datos, Redis y diskSpace. |
| /actuator/health/liveness | Probe público del estado de vida, sin detalles. |
| /actuator/health/readiness | Probe público del estado de disponibilidad, sin detalles. |
| /actuator/info | Exige permiso administrativo; sin contribuyentes adicionales puede devolver un objeto vacío. |
| /actuator/metrics | Catálogo de métricas, solo administración. |
| /actuator/metrics/http.server.requests | Estadísticas HTTP; requiere al menos una petición registrada. |
| /actuator/metrics/cache.gets?tag=name:categorias&tag=result:hit | Aciertos locales de la caché de categorías del proceso backend. |

Sin token, los endpoints administrativos responden 401; con vendedor, 403.
Las peticiones de salud autenticadas como vendedor tampoco revelan detalles.
RequestIdFilter sigue añadiendo X-Request-Id al monitoreo, pero no genera sus líneas
de acceso. Las probes conservan sus indicadores predeterminados; la caída de Redis
se refleja en la salud global, sin equiparar ese fallo al estado de vida del proceso.

## Comprobaciones realizadas

- Salud pública UP sin detalles; admin ve base de datos y diskSpace, vendedor no.
- Acceso a info, catálogo de métricas y http.server.requests: anónimo 401,
  vendedor 403 y admin 200. Se usa la cadena real JWT en la prueba de seguridad.
- Probes liveness y readiness públicas y sin detalles.
- Con Redis real, salud indica Redis UP y cache.gets registra un hit positivo
  después de repetir una consulta de categorías.
- Con Redis configurado hacia un puerto inaccesible, salud devuelve DOWN/503,
  oculta los componentes al público y permite al admin distinguir Redis DOWN de
  base de datos UP. El CRUD de categorías mantiene el fallback de base de datos.

Resultado final: 88 casos contabilizados, 78 aprobados, cero fallos/errores y
10 omitidos que requieren integración Oracle habilitada expresamente.
Las bases utilizadas son H2. Los tests de Redis real usan database 15 y claves
con prefijo s13-test::; la prueba de fallo apunta al puerto 1, sin detener Redis.
La prueba de seguridad desactiva solo su indicador Redis para no depender del
contenedor al ejecutar las pruebas habituales; la integración específica sí lo verifica.

La configuración Oracle y sus datos no se modificaron. Las capturas y mediciones
manuales se realizarán después del despliegue. El registro de ejecución está en
target/s13-actuator-tests.log y no se versiona.

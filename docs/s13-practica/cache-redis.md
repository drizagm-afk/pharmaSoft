# S13: caché Redis del catálogo

Fecha: 5 de noviembre de 2026. Primer bloque del backend.

## Configuración y claves

Redis 7 está disponible en el contenedor pharma-redis, publicado solo en
127.0.0.1:6379. REDIS_HOST y REDIS_PORT permiten cambiar la conexión.
Los tiempos de conexión y comando son 500 ms; las entradas tienen TTL de 10 minutos
y estadísticas habilitadas. No se modifican tablas ni migraciones de Oracle.

| Caché | Claves |
| --- | --- |
| categorias | todas; id-{id} |
| productos | todas; id-{id}; {pagina}-{tamanio}-{ordenarPor}-{direccion} |

CategoriaResponseDTO, ProductoResponseDTO y PaginaResponse implementan Serializable.
Se utiliza la serialización predeterminada de Spring Data Redis para los DTO,
incluyendo las listas, los importes y las fechas.

## Invalidación y transacciones

- Crear o eliminar categoría limpia categorias; actualizar limpia categorias y
  productos porque los productos incluyen el nombre de su categoría.
- Crear, editar o dar de baja productos limpia todas las variantes de productos.
- Registrar una venta confirma la transacción y después limpia productos.
  Un fallo de la venta revierte los descuentos y no invalida el resultado vigente.
- El interceptor de caché envuelve las transacciones iniciadas por los servicios:
  la escritura de caché o su invalidación se ejecuta después de que termine
  correctamente la transacción del método.
- Las lecturas de stock que deciden una venta siguen en el repositorio, no en Redis.
  La caché no sustituye las comprobaciones de disponibilidad ni la autorización JWT.

CacheConfig fuerza immediateWrites en el escritor Redis. En la primera integración
se detectó que las escrituras asíncronas podían cruzarse con una invalidación y dejar
una respuesta anterior disponible. Con escrituras inmediatas, los seis casos de
integración Redis pasan sin esperas artificiales ni reintentos en los tests.
Se conserva la autoconfiguración de TTL, prefijos y estadísticas de Spring Boot.

Referencia técnica: [cambios del contrato de RedisCache en Spring Data 2025.1](https://github.com/spring-projects/spring-data-commons/wiki/Spring-Data-2025.1-Release-Notes#cache-contract-refinements-in-rediscache).

## Redis no disponible

CacheErrorHandler registra WARN sin valores, credenciales ni texto de conexión.
Los fallos de lectura, escritura o invalidación no convierten una operación comercial
válida en un error de caché. Las lecturas consultan la base de datos cuando Redis falla.

La invalidación no tiene una cola persistente: si se modifica información mientras
Redis está inaccesible y el servidor Redis conserva datos antiguos al recuperarse,
hay que limpiar las cachés afectadas antes de volver a utilizarlas; el TTL limita
su permanencia. La comprobación manual de caída usará consultas sin escrituras.

## Verificación

Resultado final: 75 casos contabilizados, 65 aprobados, cero fallos/errores y
10 omitidos que requieren la integración Oracle habilitada expresamente.

- Seis pruebas con Redis real y H2 aislado: hit sin SQL adicional, TTL entre
  1 y 600 segundos, serialización de páginas, claves por tamaño/orden, invalidación
  tras escrituras, renombrado de categorías, stock tras venta y rollback.
- Una prueba con Redis deliberadamente inaccesible en el puerto 1 confirma que
  crear, leer, actualizar y eliminar categorías siguen funcionando con H2.
- Una prueba de fallos inyectados confirma que los errores get, put y clear no
  interrumpen la ejecución del servicio.
- La regresión de seguridad usa caché deshabilitada para no compartir resultados
  de transacciones de prueba revertidas ni requerir Redis.

Las pruebas Redis se activan con la propiedad Maven pharma.cache.integration=true.
Usan H2, Redis database 15 y el prefijo s13-test::; limpian exclusivamente sus propias
cachés. La base Oracle, las cuentas, las ventas y el stock de clase no se modificaron.
El log de ejecución está en target/s13-cache-tests.log, ignorado por Git.

Las capturas manuales y el tiempo HTTP después de optimizar siguen pendientes.
Actuator y la correlación X-Request-Id pertenecen a los próximos bloques.

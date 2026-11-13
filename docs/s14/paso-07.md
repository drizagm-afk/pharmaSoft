# S14 - Paso 7: bitácora de eventos

## Implementación

La entidad AuditoriaEvento utiliza la tabla auditoria_eventos creada por V4. El repositorio ofrece filtros opcionales de usuario, acción y fechas. AuditoriaEventoDTO expone los ocho campos del registro.

EventoAuditoria captura el usuario y el requestId mientras la petición sigue activa. El detalle se limita a 500 caracteres y el usuario de login a 50. Los eventos de login no contienen contraseña ni token.

Los servicios publican estos eventos:

| Acción | Momento y detalle |
|---|---|
| VENTA_REGISTRADA | Venta guardada: total y número de líneas de detalle. |
| PRECIO_MODIFICADO | Precio anterior y nuevo; solo cuando compareTo detecta un cambio numérico. |
| PRODUCTO_ELIMINADO | Baja lógica confirmada: nombre e ID del producto. |
| LOGIN_EXITOSO | Autenticación y generación de token correctas; usuario canónico de la cuenta. |
| LOGIN_FALLIDO | AuthenticationManager rechaza la autenticación; se conserva la respuesta de error original. |

AuditoriaListener atiende los eventos de negocio después del commit. AuditoriaService.registrar abre una transacción independiente REQUIRES_NEW. Un rollback de negocio descarta el evento pendiente. Los errores de almacenamiento de auditoría se registran sin propagar una excepción a una operación ya confirmada. Los logins llaman directamente al servicio independiente porque el intento fallido termina en error.

## Consulta administrativa

```http
GET /api/v1/auditoria?usuario=admin&accion=LOGIN_EXITOSO&desde=2026-11-13&hasta=2026-11-13&pagina=0&tamanio=20
```

Todos los filtros son opcionales. El usuario se compara sin distinguir mayúsculas; la acción se compara exactamente. Los espacios exteriores se eliminan y los filtros vacíos se tratan como ausentes.

El rango incluye todo el día hasta, usando un límite exclusivo al inicio del día siguiente. El orden es fecha descendente con ID como desempate. Se reutiliza PaginaResponse y la validación existente de página y tamaño: página desde cero y tamaño entre 1 y 100; valores predeterminados 0 y 20.

La ruta exige USUARIOS_ADMINISTRAR. Una petición sin sesión recibe 401; un usuario autenticado sin ese permiso recibe 403. Un rango invertido o una paginación fuera de límites recibe 409 siguiendo las reglas existentes; una fecha con formato inválido recibe 400.

## Verificación

Fecha: 13 de noviembre de 2026.

| Prueba | Resultado |
|---|---|
| Venta confirmada mediante MockMvc y servicios reales | Evento con usuario, ID de venta, total, ítems y requestId idéntico a la cabecera HTTP. Stock descontado. |
| Venta revertida después de publicar el evento | Sin venta, sin evento y stock original conservado. |
| Venta con stock insuficiente | Sin evento de venta registrada. |
| Precio sin cambio numérico | No genera evento aunque cambie la escala decimal. |
| Cambio de precio revertido | No genera evento. |
| Cambio de precio confirmado | Conserva precio anterior y nuevo y el editor de la entidad. |
| Baja lógica | Genera evento; repetir una baja rechazada no añade otro. |
| Login correcto e incorrecto | Eventos persistidos con sus requestId, sin credenciales en detalle. |
| Transacción independiente | El evento de login sobrevive al rollback de una transacción exterior. |
| Fallo al auditar tras commit | El listener no propaga el error. |
| Consulta | Filtros opcionales y combinados, usuario sin distinguir mayúsculas, día completo, exclusión de medianoche siguiente, paginación y respuesta vacía. |
| Seguridad y parámetros | Acceso administrativo; 401/403 y errores de parámetros comprobados. |
| Suite general | 102 pruebas descubiertas: 84 aprobadas, 18 opcionales omitidas, 0 fallos y 0 errores. |
| Redis real | 7 pruebas aprobadas: serialización e invalidación conservadas al incorporar eventos. |
| Backend conectado a Oracle | Health UP y endpoint presente en OpenAPI. Peticiones anónimas directas y por Nginx: 401. |

Las pruebas de escritura usan H2 aislado. BitacoraIntegrationTest realiza commits y rollbacks reales dentro de esa base y elimina sus registros al terminar. Las comprobaciones del servidor conectado a Oracle son de lectura y no registran ventas ni cambios de catálogo.

Comandos ejecutados desde la raíz del backend:

```text
./mvnw.cmd -o -Dspring.profiles.active=dev test
./mvnw.cmd -o -Dspring.profiles.active=dev -Dpharma.cache.integration=true -Dtest=CatalogoCacheIntegrationTest test
```

Logs locales: target/s14-paso07-tests.log y target/s14-paso07-cache-tests.log.

## Siguiente etapa

El paso 8 recorre la matriz I-01 a I-08 con vendedor y administrador. Las verificaciones de esta etapa comprueban la implementación de la bitácora; la matriz de integración y sus evidencias se realizarán en la etapa siguiente.

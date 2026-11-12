# S14 - Paso 5: migración de auditoría

## Cambios

- V4__auditoria.sql agrega creado_por y modificado_por a categorías, productos y clientes, y creado_por a ventas.
- Las siete columnas son VARCHAR2(50 CHAR), opcionales para conservar los registros anteriores sin atribuirles un autor inventado.
- auditoria_eventos incorpora ID identity, fecha, usuario, acción, entidad, entidad_id, detalle y request_id, con su clave primaria.
- Índices: ix_auditoria_fecha(fecha), ix_auditoria_usuario(usuario, fecha) e ix_productos_nombre(nombre).

No se modificaron V1 ni V2. El repositorio y el esquema inspeccionados no tenían V3; se conserva el nombre V4 solicitado por la guía, sin inventar una migración histórica. El salto de versión quedó validado por Flyway.

## Verificación en Oracle de desarrollo

Fecha: 12 de noviembre de 2026.

| Comprobación | Resultado |
|---|---|
| Historial anterior | Baseline 1 y migración 2 correctos. |
| Aplicación de V4 | Flyway la aplicó durante el reinicio automático de desarrollo a las 10:52:34. |
| Historial posterior | V4 auditoria registrada una vez con success=1. |
| Productos | 5011 antes y después. |
| Ventas | 20012 antes y después. |
| Columnas y longitudes | Siete columnas de autor, VARCHAR2(50 CHAR), nullable. |
| Tabla de eventos | Ocho columnas; identity y clave primaria verificadas. |
| Índices | Los tres índices existen, son VALID y conservan el orden de columnas esperado. |
| Repetición de Flyway | Sin migraciones pendientes; no repite DDL. |

El test AuditoriaMigrationOracleTest valida el esquema real sin arrancar otro servidor web. Se activa expresamente con la propiedad pharma.integration=true y lee la conexión de desarrollo, sin imprimir la contraseña.

```text
./mvnw.cmd -o -Dpharma.integration=true -Dtest=AuditoriaMigrationOracleTest test
```

Resultado: 1 prueba aprobada, sin fallos, errores ni omisiones. Los snapshots de metadatos antes/después y el resultado de la prueba están en resultados/.

## Alcance del siguiente paso

Este paso prepara el esquema. El llenado automático con el usuario del token, los campos en entidades/DTO/mappers y la configuración JPA corresponden al paso 6. La publicación de eventos y su consulta protegida corresponden al paso 7.

V4 ya está aplicada en desarrollo: no ejecutar manualmente sus ALTER TABLE en DBeaver ni editar el archivo después del commit. Las correcciones futuras de una migración aplicada deben ir en una versión nueva.

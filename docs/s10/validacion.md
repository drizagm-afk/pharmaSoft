# Validación de seguridad - 15/10/2026

Comando ejecutado: `./mvnw.cmd test -Dpharma.integration=true`.

| Pruebas | Ejecutadas | Fallos | Errores | Omitidas |
|---|---:|---:|---:|---:|
| ReglasTest | 4 | 0 | 0 | 0 |
| JwtServiceTest | 5 | 0 | 0 | 0 |
| PermisosPorRolTest | 4 | 0 | 0 | 0 |
| SeguridadIntegrationTest (H2 aislado) | 27 | 0 | 0 | 0 |
| OracleSecurityIntegrationTest | 1 | 0 | 0 | 0 |
| VentasIntegrationTest (Oracle) | 9 | 0 | 0 | 0 |
| Total | 50 | 0 | 0 | 0 |

Oracle: baseline de esquema existente en versión 1 y aplicación de V2 completados. Hibernate validó el esquema. Se verificaron los cuatro logins activos del material y el rechazo de la cuenta inactiva contra Oracle. Las pruebas de ventas eliminaron únicamente sus fixtures temporales.

Recuento de ventas de trabajo antes y después: **8 → 8**. Tablas EMPLEADOS, ROLES, USUARIOS y USUARIO_ROLES, más historial Flyway, presentes después de migrar. No se crearon ventas de demostración adicionales ni se registró la cuenta de María: queda disponible para el alta de usuario de la prueba manual.

El flujo de dos roles se comprobó con actualización de producto HTTP 200 y registro de venta HTTP 201. El controlador de productos devolvía antes 201 al actualizar; se corrigió a 200 conforme al contrato de la sesión. El 403 de reportes se comprobó a través de la anotación de autorización por método.

Las pruebas automáticas no reemplazan las capturas de las 24 comprobaciones manuales exigidas por la guía. Las evidencias y el informe se prepararán por separado. No se hicieron commits ni se creó una solicitud de incorporación.

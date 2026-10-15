# Seguridad backend - sesión 10

## Ejecutar

Java 25 y Oracle en la conexión configurada en application-dev.yaml. La rama de trabajo es `feature/seguridad-jwt`. Mantener las credenciales de conexión locales que ya usa el proyecto.

```powershell
./mvnw.cmd test
./mvnw.cmd spring-boot:run
```

Swagger: http://localhost:8080/swagger-ui.html. Health público: `/api/health` y `/api/v1/health`.

Flyway administra ahora el esquema. Para la base de desarrollo existente sin historial, `baseline-on-migrate: true` registra la versión 1 y ejecuta V2, sin borrar ventas, clientes ni productos. En un esquema nuevo, V1 crea el modelo comercial y V2 crea seguridad. Hibernate valida el esquema en lugar de alterarlo. No editar una migración que ya se haya aplicado.

Si el esquema ya contiene tablas de seguridad creadas manualmente, no arrancar suponiendo que están migradas: contrastar primero su definición e historial con V2. En producción no se activa el baseline automático; un esquema previo requiere un baseline explícito después de revisar su estructura.

## Login y perfiles de clase

Las cuentas de demostración de V2 son exclusivamente para desarrollo y prácticas:

| Usuario | Contraseña de clase | Roles |
|---|---|---|
| admin | Admin2026* | ADMIN |
| vendedor | Vendedor2026* | VENDEDOR |
| almacen | Almacen2026* | ALMACENERO |
| supervisor | Supervisor2026* | VENDEDOR y ALMACENERO |
| inactivo | Inactivo2026* | VENDEDOR, deshabilitado |

```http
POST /api/v1/auth/login
Content-Type: application/json

{"username":"vendedor","password":"Vendedor2026*"}
```

La respuesta incluye token, tipo Bearer, duración en segundos, nombre, roles y permisos. Usar `Authorization: Bearer <token>` en cada petición protegida. En Authorize de Swagger pegar solamente el token. `GET /api/v1/auth/me` devuelve el perfil sin contraseña ni hash.

Los permisos se recalculan desde la base en cada petición: desactivar una cuenta o su empleado revoca el acceso con sus tokens previos. Los roles actuales, no los permisos de un token antiguo, deciden la autorización.

## Permisos

| Aspecto | ADMIN | VENDEDOR | ALMACENERO | Ambos roles |
|---|---|---|---|---|
| Leer catálogo | Sí | Sí | Sí | Sí |
| Editar catálogo | Sí | No | Sí | Sí |
| Clientes y ventas | Sí | Sí | No | Sí |
| Reportes | Sí | No | No | No |
| Usuarios, empleados y roles | Sí | No | No | No |

El acceso a reportes está protegido con `@PreAuthorize`. La API responde 401 sin autenticación válida y 403 cuando la cuenta autenticada carece de permiso, usando el formato ErrorResponseDTO. Credenciales incorrectas, cuentas inexistentes e inactivas comparten el mismo mensaje de login.

## Alta de usuario

Como admin, consultar `/api/v1/empleados` y `/api/v1/roles` para obtener identificadores reales. María Huamán comienza sin cuenta. Enviar POST `/api/v1/usuarios` con username, password, empleadoId y rolIds. Debe existir un empleado activo sin usuario y uno o más roles válidos. La contraseña necesita 8-72 caracteres, letras y números, y no más de 72 bytes UTF-8. Se persiste únicamente el hash BCrypt; la respuesta no contiene hash ni contraseña. Índices de V2 garantizan username sin duplicados de mayúsculas y empleado único aun ante solicitudes concurrentes.

## Configuración

En desarrollo se incluye la clave JWT de demostración del material de clase. Puede reemplazarse con `JWT_SECRET` (Base64, al menos 32 bytes). Producción exige `JWT_SECRET`, `JWT_EXPIRACION_MINUTOS` (30 por defecto), variables DB y `CORS_ALLOWED_ORIGINS`; no usar cuentas o claves de demostración en un entorno real.

La SPA anterior no incluye todavía autenticación. Recibir 401 desde esa SPA es esperado después de proteger la API; la integración del frontend corresponde al siguiente trabajo.

## Verificar

- `JwtServiceTest`: claims, firma alterada, otra clave, expiración y duración.
- `PermisosPorRolTest`: permisos por rol, unión y rol desconocido.
- `SeguridadIntegrationTest`: cadena HTTP real y servicios JPA sobre H2 aislado; login, perfil, permisos, alta BCrypt, validaciones, rechazo de tokens, revocación y CORS. Cada prueba revierte sus datos; no usa el esquema Oracle de trabajo.
- `VentasIntegrationTest`: regresión Oracle optativa con `-Dpharma.integration=true`; ahora ejecuta las peticiones como usuario autorizado. No confundir una prueba omitida por esa precondición con una prueba aprobada.

Las 24 pruebas manuales y sus capturas se realizarán al preparar las evidencias. No se han creado commits, solicitud de incorporación ni entregas desde esta implementación.

Referencias técnicas: [Spring Security: DaoAuthenticationProvider](https://docs.spring.io/spring-security/reference/7.0/servlet/authentication/passwords/dao-authentication-provider.html), [módulos de Spring Boot 4](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide).

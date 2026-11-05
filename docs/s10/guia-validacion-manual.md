# Sesión 10 - guía de validación y capturas

Basada en las 24 pruebas del paso 14 de la guía práctica, páginas 34-36. Ejecutar manualmente desde Postman o Swagger. La numeración S10-01 a S10-24 corresponde a la numeración de la guía; una prueba puede necesitar más de una captura.

## 0. Estado inicial y qué se ha ejecutado

Lectura de Oracle del 15/10/2026:

- Flyway: esquema existente baselined en V1, V2 de seguridad aplicada. Los cuatro usuarios activos pueden iniciar sesión; inactivo no puede hacerlo.
- Seis empleados, cinco usuarios y tres roles. María Huamán, DNI 45667788, empleado **6**, sigue SIN cuenta. Rol VENDEDOR = **2**, ADMIN = 1, ALMACENERO = 3. Estos empleados son distintos de los clientes de ventas.
- Cliente de ventas de trabajo: **11**, María Elena Huamán Flores, DNI 90000001.
- Vitamina D, producto **133**, S/12.50, stock **21**. Protector solar, producto **134**, S/18.90, stock **19**, categoría 11.
- **Ocho ventas** de trabajo. Las pruebas anteriores crearon y retiraron sus propios datos temporales: no registraron mhuaman ni consumieron el stock de estos productos. Los identificadores de nuevas ventas pueden tener saltos por esas pruebas; no asumir el próximo número.

No hace falta revertir código, eliminar usuarios, borrar historial Flyway ni reconstruir la base. Las pruebas del paso 14 pueden hacerse sobre este estado. Los logins anteriores no consumen una oportunidad de validación.

La primera aplicación de V2 ya ocurrió. Para documentar la migración se puede capturar el historial y un arranque que valide V2. No presentar ese nuevo arranque como la primera creación de tablas. Volver a crear una base únicamente para repetir ese momento no es necesario para las 24 pruebas.

## 1. Preparación de Postman y capturas

1. Arranca Oracle y reinicia el backend con su configuración normal (expiración 60 minutos). Abre Swagger o Postman. El frontend no hace falta para estas pruebas.
2. En Postman importa `PharmaBackend-S10-Manual.postman_collection.json`, situado junto a esta guía. Contiene las peticiones, los cuerpos y guardado automático de tokens. Alternativamente crea una colección «S10 Seguridad» manualmente. URL base: `http://localhost:8080`. En todos los POST/PUT de JSON: Body > raw > JSON (`Content-Type: application/json`).
3. Crea variables locales de colección: `tokenVendedor`, `tokenAlmacen`, `tokenAdmin`, `tokenSupervisor`; inicialmente vacías. Introduce cada token manualmente después del login, o usa la colección de apoyo preparada junto a esta guía.
4. Authorization por petición: **No Auth** para health, login y pruebas sin token; **Bearer Token** y la variable del usuario indicado para las protegidas. No dejar una autorización heredada cuando se prueba No Auth.
5. Guarda evidencias en una carpeta NUEVA, por ejemplo `Evidence/S10`. Nombres sugeridos `S10-01`, `S10-17a`, etc. Los nombres no aparecerán como títulos de archivo en el informe.
6. En cada captura incluye método, URL, código HTTP y cuerpo de respuesta legible. Cuando haga falta otra imagen, captura el request Body o la pestaña Authorization por separado. Captura ANTES de reutilizar la petición; guardar una colección no conserva automáticamente sus respuestas.
7. No recortes el código HTTP ni la URL. Para tokens largos basta que se vea el campo token y los demás datos de respuesta; no hace falta meter todo el JWT en una sola línea.
8. Al cambiar de rol revisa el token elegido. Si transcurre más de una hora, renueva los tokens que necesites mediante login.

La colección contiene 31 peticiones porque incluye logins auxiliares y pruebas con más de una operación. Envíalas **una a una**, sin Collection Runner ni Send All: registrar ventas, crear la cuenta y esperar a la expiración requieren controlar el momento de cada paso. La prueba S10-07 se realiza en consola, no mediante una petición HTTP.

Credenciales de clase:

| Usuario | Contraseña | Uso |
|---|---|---|
| admin | Admin2026* | Reportes y administración |
| vendedor | Vendedor2026* | Clientes, lectura de catálogo y ventas |
| almacen | Almacen2026* | Catálogo |
| supervisor | Supervisor2026* | Unión VENDEDOR + ALMACENERO |
| inactivo | Inactivo2026* | Rechazo de login |

Para obtener cualquier token auxiliar, POST `/api/v1/auth/login` con username y password correspondientes, No Auth. Guarda el token sin la palabra Bearer en la variable respectiva. Los logins auxiliares no reemplazan las pruebas de login numeradas.

## Grupo A. Rutas públicas y autenticación - S10-01 a S10-06

### S10-01 - Health público
GET `http://localhost:8080/api/health`, **No Auth**. Esperado **200**. Captura código y respuesta.

### S10-02 - Catálogo sin token
GET `/api/v1/productos`, **No Auth**. Esperado **401**, message `Debe iniciar sesión para acceder a este recurso`. Captura también el formato timestamp/status/error/message/path/validationErrors. Puedes mostrar `WWW-Authenticate: Bearer` en Headers como complemento.

### S10-03 - Contraseña incorrecta
POST `/api/v1/auth/login`, No Auth:

```json
{"username":"admin","password":"Incorrecta2026*"}
```

Esperado **401**, `Usuario o contraseña incorrectos`. Captura request y respuesta.

### S10-04 - Cuenta inactiva
Misma URL, No Auth:

```json
{"username":"inactivo","password":"Inactivo2026*"}
```

Esperado **401**, exactamente el mismo mensaje que S10-03. Captura request y respuesta.

### S10-05 - Validación de username
Misma URL, No Auth:

```json
{"username":"","password":"Vendedor2026*"}
```

Esperado **400**, `validationErrors.username`. Captura request y respuesta.

### S10-06 - Login vendedor
Misma URL, No Auth:

```json
{"username":"vendedor","password":"Vendedor2026*"}
```

Esperado **200**, token, tipo Bearer, expiraEnSegundos **3600**, username vendedor, roles `["VENDEDOR"]` y permisos CATALOGO_LEER, CLIENTES_LEER, CLIENTES_ESCRIBIR, VENTAS_LEER, VENTAS_REGISTRAR. No REPORTES_VER ni CATALOGO_ESCRIBIR. Captura y guarda el token en `tokenVendedor`.

## Grupo B. JWT, perfil y vendedor - S10-07 a S10-12

### S10-07 - Leer claims localmente
En la consola del navegador ejecuta lo siguiente, pegando el token de S10-06 dentro de las comillas. Es una decodificación LOCAL, sin enviarlo a una web externa:

```javascript
const jwtS10 = 'PEGA_AQUI_EL_TOKEN';
const segmentoS10 = jwtS10.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
const base64S10 = segmentoS10.padEnd(Math.ceil(segmentoS10.length / 4) * 4, '=');
const claimsS10 = JSON.parse(new TextDecoder().decode(
  Uint8Array.from(atob(base64S10), c => c.charCodeAt(0))
));
console.log(JSON.stringify(claimsS10, null, 2));
console.log('Duración en segundos:', claimsS10.exp - claimsS10.iat);
```

Captura sub vendedor, authorities, iat, exp y diferencia **3600**. Leer claims no valida la firma: la validación de firma la hace el backend.

### S10-08 - Perfil autenticado
GET `/api/v1/auth/me`, Bearer `{{tokenVendedor}}`. Esperado **200**, perfil de vendedor con datos planos del empleado, roles/permisos, sin password ni hash. Captura respuesta completa, desplazando si es necesario.

### S10-09 - Leer catálogo como vendedor
GET `/api/v1/productos`, token vendedor. Esperado **200** paginado. Captura contenido y metadatos, o dos imágenes si no caben.

### S10-10 - Borrado denegado
DELETE `/api/v1/productos/1`, token **vendedor**. Esperado **403**, `No tiene permisos para realizar esta operación`. El caso usa el id 1 de la guía; la autorización se rechaza antes de buscarlo, así que no necesita existir. No sustituir el token por admin para ejecutar esta petición.

### S10-11 - Registrar una venta como vendedor
POST `/api/v1/ventas`, token vendedor:

```json
{"clienteId":11,"detalles":[{"productoId":133,"cantidad":1}]}
```

Esperado **201**, REGISTRADA, total **12.50**, detalle Vitamina D x1. Captura Body, respuesta y número real de venta. Esta prueba SÍ registra una venta y baja Vitamina D **21 → 20**, si no hubo operaciones adicionales. Anota ese número; no repitas una venta aprobada solo para volver a capturar.

### S10-12 - Reporte denegado al vendedor
GET `/api/v1/reportes/ventas-por-categoria`, token vendedor. Esperado **403**. Captura respuesta. Para mostrar que la protección es por método, añade una captura de ReporteController con `@PreAuthorize("hasAuthority('REPORTES_VER')")` y otra de la regla final autenticada en SecurityConfig si deseas documentar la implementación. No modificar código.

## Grupo C. Almacén, admin y roles múltiples - S10-13 a S10-18

Antes de S10-13, inicia sesión como almacen y guarda `tokenAlmacen`. Antes de S10-15, como admin y guarda `tokenAdmin`. Guarda estas respuestas auxiliares también.

### S10-13 - Almacén no vende
POST `/api/v1/ventas`, token almacen, mismo Body de S10-11. Esperado **403**. No se crea venta ni se descuenta stock. Captura token/usuario identificado en tu organización de peticiones y respuesta.

### S10-14 - Almacén actualiza producto
Primero GET `/api/v1/productos/134` con token almacen. Guarda su estado actual. Después PUT `/api/v1/productos/134` con un Body que conserve los valores actuales. Si no han cambiado:

```json
{
  "nombre":"Protector solar",
  "descripcion":"Protector solar marca NIVEA",
  "precio":18.90,
  "stock":19,
  "categoriaId":11,
  "estado":true
}
```

Esperado **200** y producto actualizado. Si GET devuelve otro nombre, descripción, precio o stock, usa los valores reales: no restablecer stock de una lectura antigua. Reenviar los valores actuales comprueba el permiso sin cambiar intencionadamente precio o existencias; fechaModificacion sí puede actualizarse. Captura request y respuesta.

### S10-15 - Admin ve el reporte
GET `/api/v1/reportes/ventas-por-categoria`, token admin. Esperado **200**, reporte por categoría. Captura código y respuesta; sus importes incluyen las ventas nuevas que hayas registrado.

### S10-16 - Login con dos roles
POST `/api/v1/auth/login`, No Auth:

```json
{"username":"supervisor","password":"Supervisor2026*"}
```

Esperado **200**, roles ALMACENERO y VENDEDOR; unión de permisos sin repetidos, incluyendo CATALOGO_ESCRIBIR y VENTAS_REGISTRAR, pero no REPORTES_VER. Guarda en `tokenSupervisor`. Captura roles y permisos.

### S10-17 - Supervisor puede vender Y editar
1. POST `/api/v1/ventas`, token supervisor, Body de S10-11. Esperado **201**, total **12.50**. Captura S10-17a y anota la nueva venta. Vitamina D baja **20 → 19** si solo se hicieron las dos ventas indicadas.
2. GET `/api/v1/productos/134` con token supervisor y después PUT con sus valores actuales, como S10-14. Esperado **200**. Captura S10-17b.
3. Son dos operaciones distintas bajo el mismo usuario: guarda ambas respuestas. Con las dos ventas válidas, el total de ventas de trabajo pasa **8 → 10**.

### S10-18 - Supervisor no ve reportes
GET `/api/v1/reportes/ventas-por-categoria`, token supervisor. Esperado **403**. Tener los dos roles no concede permisos de ADMIN.

## Grupo D. Administración y relaciones - S10-19, S10-22, S10-20, S10-21

**Orden deliberado: 19 → 22 → 20 → 21.** El servicio valida primero si el empleado ya tiene cuenta. Si haces 20 y luego 22 con empleado 6, el rechazo sería 409 por cuenta existente antes de llegar a los roles. Hacer 22 antes de 20 permite capturar el 404 de rol inexistente sin eliminar la cuenta creada. Mantén las etiquetas originales de las pruebas para el informe.

### S10-19 - Empleados y roles
Con token admin:
1. GET `/api/v1/empleados`: esperado **200**; captura María Huamán, DNI 45667788, id **6**, `tieneUsuario: false`.
2. GET `/api/v1/roles`: esperado **200**; captura roles, ids y permisos, especialmente VENDEDOR id **2**.
3. Si estos valores difieren, detente antes de crear la cuenta y usa los identificadores reales. Si María ya tiene cuenta, consulta primero qué operación la creó: no borrar cuentas automáticamente.

### S10-22 - Rol inexistente (hacer ANTES de S10-20)
POST `/api/v1/usuarios`, token admin:

```json
{"username":"mhuaman","password":"Cajera2026","empleadoId":6,"rolIds":[2,99]}
```

Esperado **404**, `Roles no encontrados: [99]`. Antes verifica que rol 99 no exista en tu base; actualmente solo existen 1, 2 y 3. Captura Body y respuesta. Este intento no crea usuario, por lo que María sigue libre para S10-20.

### S10-20 - Crear cuenta y comprobar BCrypt
POST `/api/v1/usuarios`, token admin:

```json
{"username":"mhuaman","password":"Cajera2026","empleadoId":6,"rolIds":[2]}
```

Esperado **201**, roles `["VENDEDOR"]`, empleado 6, sin password en respuesta. Captura Body y respuesta. En Oracle ejecuta estas consultas SOLO de lectura:

```sql
SELECT id, username, empleado_id,
       SUBSTR(password, 1, 7) AS prefijo_bcrypt,
       LENGTH(password) AS longitud_hash
FROM usuarios
WHERE LOWER(username) = 'mhuaman';

SELECT u.username, u.empleado_id, ur.usuario_id, ur.rol_id, r.nombre AS rol
FROM usuarios u
JOIN usuario_roles ur ON ur.usuario_id = u.id
JOIN roles r ON r.id = ur.rol_id
WHERE LOWER(u.username) = 'mhuaman';
```

Captura prefijo **$2a$10$**, longitud **60** y la relación al rol VENDEDOR. No necesitas exponer todo el hash. Ahora existen seis usuarios y siete asignaciones usuario_roles si no hubo otras altas.

### S10-21 - Un empleado no puede tener dos cuentas
POST `/api/v1/usuarios`, token admin:

```json
{"username":"mhuaman2","password":"Cajera2026","empleadoId":6,"rolIds":[2]}
```

Esperado **409**, mensaje que indica que el empleado ya tiene un usuario. Captura request y respuesta. No cambiar empleadoId, porque es precisamente la relación 1 a 1 que se valida.

## Grupo E. Integridad y expiración del token - S10-23 a S10-24

### S10-23 - Alterar la firma
1. Renueva el login vendedor si tu token está cerca de expirar. Conserva `tokenVendedor` válido.
2. En la consola local, ejecuta (con un nombre nuevo o reutilizando la variable anterior para evitar errores de redeclaración):

```javascript
var partesAlteradasS10 = 'PEGA_TOKEN_VALIDO'.split('.');
partesAlteradasS10[2] =
  (partesAlteradasS10[2][0] === 'A' ? 'B' : 'A') + partesAlteradasS10[2].slice(1);
console.log(partesAlteradasS10.join('.'));
```

3. Copia el token alterado a una petición independiente: GET `/api/v1/productos`, Bearer con ese valor. Esperado **401**, `El token no es válido`. Cambiar el PRIMER carácter de la firma evita que un cambio en los últimos bits Base64 sea irrelevante. No sustituyas el token válido guardado para los otros casos.
4. Captura respuesta. No basta quitar Bearer o enviar cualquier texto: aquí se demuestra alteración de un JWT firmado.

### S10-24 - Token realmente expirado
No uses el token de una hora anterior para este caso: debe emitirse DESPUÉS de arrancar con duración de un minuto.

1. Detén el backend que ocupa 8080. No detengas Oracle.
2. En una terminal PowerShell en la carpeta del backend, ejecuta:

```powershell
$env:JWT_EXPIRACION_MINUTOS = '1'
./mvnw.cmd spring-boot:run
```

3. Haz un login vendedor nuevo, No Auth. Comprueba `expiraEnSegundos: 60`. Guarda el token por separado como `tokenCorto` y captura la respuesta.
4. Inmediatamente GET `/api/v1/productos` con tokenCorto: esperado **200**, captura S10-24a.
5. Espera **dos minutos completos desde la emisión**, sin volver a iniciar sesión ni actualizar tokenCorto. Repite exactamente ese GET con el MISMO token: esperado **401**, `El token ha expirado. Inicie sesión nuevamente`. Captura S10-24b. Conserva la captura del login de 60 segundos para vincular la prueba.
6. Detén ese backend con Ctrl+C y restaura la configuración normal en la MISMA terminal:

```powershell
Remove-Item Env:JWT_EXPIRACION_MINUTOS
./mvnw.cmd spring-boot:run
```

7. Nuevo login vendedor: confirma `expiraEnSegundos: 3600`, captura final opcional. Si habías configurado antes otro valor de esa variable, restáuralo en vez de eliminarlo. Los tokens cortos ya emitidos conservan su vencimiento; reiniciar no los alarga.

## Complementos de entrega: Flyway y pruebas unitarias

La guía también pide las pruebas unitarias en verde y el versionado, además de las 24 respuestas.

### Flyway sin repetir migraciones

```sql
SELECT "installed_rank", "version", "description", "type", "script", "success"
FROM "flyway_schema_history"
ORDER BY "installed_rank";
```

Captura baseline 1 y SQL V2 exitoso; al reiniciar captura los mensajes de validación y esquema actualizado a versión 2. No ejecutar DELETE sobre el historial, DROP de tablas, clean ni repair para recrear capturas.

### Unitarias sin modificar Oracle

Desde la carpeta del backend:

```powershell
./mvnw.cmd test '-Dtest=JwtServiceTest,PermisosPorRolTest'
```

Captura nombres, resumen y BUILD SUCCESS: **nueve pruebas** (cinco JWT y cuatro permisos). Este comando no levanta el contexto Oracle ni repite la migración. Para todas las comprobaciones automatizadas usa `./mvnw.cmd test`; Oracle es optativo con `-Dpharma.integration=true`, no necesario para tus capturas manuales.

### Versionado

Captura rama y commits cuando hayas guardado los cambios, y la solicitud hacia master cuando se prepare. No crear commits vacíos ni cambiar fechas. La guía pide al menos cuatro commits convencionales. Esta guía no crea commits, solicitudes ni publicaciones.

## Qué enviar para elaborar el documento

- Capturas S10-01 a S10-24; para S10-17, dos respuestas; para S10-19, empleados y roles; para S10-20, HTTP y consultas BCrypt/relación; para S10-24, login de 60 segundos, 200 inicial y 401 final.
- Captura local de claims en S10-07, sin necesidad de PDF.
- Historial Flyway, prueba unitaria y evidencias de versionado según corresponda.
- Nota breve con ids reales de las dos ventas nuevas y cualquier resultado distinto. No repetir un POST aprobado solamente por haber olvidado una captura: conserva la respuesta o consulta el registro existente y registra qué ocurrió.

Se agruparán en el informe por autenticación, permisos, roles múltiples, administración y tokens, con títulos y descripciones, párrafos negros y encabezados azules/dorados. Los resultados se documentarán según lo que se observe en estas pruebas manuales.

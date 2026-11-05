# S10 autónoma - ejecución manual y capturas

## Antes de empezar

El backend ya implementa las tres operaciones. La verificación automática ejecutó **57 pruebas aprobadas**; otras **10 pruebas Oracle optativas se omitieron** para no tocar cuentas, credenciales ni datos de tu validación manual. Las pruebas nuevas usaron H2 aislado con rollback. No se aplicó otra migración ni se creó la cuenta de estas instrucciones.

Usa una carpeta nueva, por ejemplo `Evidence/S10Autonoma`. La matriz inicial está en `docs/matriz-seguridad-s10.md`. Sus resultados siguen pendientes hasta que ejecutes los casos y se revisen las capturas.

1. Reinicia el backend con duración normal de JWT: 60 minutos. Para los pasos con reinicios utiliza UNA terminal PowerShell en PharmaBackend. Guarda sus valores previos:

```powershell
$s10SecretAnterior = $env:JWT_SECRET
$s10ExpAnterior = $env:JWT_EXPIRACION_MINUTOS
$env:JWT_EXPIRACION_MINUTOS = '60'
./mvnw.cmd spring-boot:run
```

2. En Oracle ejecuta manualmente `docs/s10-autonoma/fixture-empleado.sql`. Solo añade un empleado de prueba con DNI **56778899**, sin cuenta. No modifica la cuenta original mhuaman. Captura la consulta final con `usuario_prueba_id` NULL y los ids de los roles.
3. Importa `postman/PharmaSoft-S10-Seguridad.json`. La colección se llama **PharmaSoft - S10 Seguridad**, baseUrl `http://localhost:8080/api/v1`. Contiene **79 peticiones**, porque varios casos necesitan más de una operación y hay restauraciones. Envía UNA a una; no uses Runner ni Send All.
4. Credenciales de clase: admin/Admin2026*, vendedor/Vendedor2026*, almacen/Almacen2026*, supervisor/Supervisor2026*, inactivo/Inactivo2026*. Los logins guardan sus tokens localmente. Las variables del archivo entregable se mantienen vacías.
5. Cada captura debe mostrar método, URL, Authorization (No Auth, Bearer o encabezado según el caso), código y cuerpo. Cuando Body y Authorization no se pueden ver juntos, guarda dos imágenes del mismo caso. Usa IDs: A-01_admin, P-01_post, B-04_antes, etc. No borres respuestas antes de capturarlas.
6. Varias peticiones de un caso cuentan como UN caso de matriz, no como casos propios adicionales. Los cuatro propios son A-05, T-06, P-05 y U-03, de grupos diferentes. B-01 a B-08 son adicionales a los 20.
7. Revisa los valores reales del catálogo antes de cualquier PUT. La colección lee producto 134 y copia solo los campos editables a productoActual; reenviar esos valores evita cambiar intencionadamente precio/stock. La fecha de modificación puede actualizarse.

## Grupo A - autenticación

Ejecuta la carpeta A completa. Todas las peticiones login usan No Auth.

| Caso | Acción | Resultado y captura |
|---|---|---|
| A-01 | Login admin, vendedor, almacen y supervisor | Cuatro respuestas 200; token, roles y permisos. Supervisor tiene VENDEDOR y ALMACENERO. Guarda las cuatro. |
| Preparación | GET /auth/me como admin | 200; guarda automáticamente usuarioAdminId. No cambia datos. |
| A-02 | Login inexistente y admin con clave incorrecta | Dos 401 con el mismo message: Usuario o contraseña incorrectos. |
| A-03 | Login inactivo con su password correcta | 401 y mismo mensaje genérico. |
| A-04 | Login VENDEDOR | 200; regla esperada: no distingue mayúsculas. La respuesta conserva username canónico vendedor. |
| A-05 propio | Login con espacios alrededor de vendedor | 200; se aplica trim al username. |

No marcar el 401 previsto como fallo: aquí es el resultado esperado.

## Grupo T - token (incluye reinicios)

### T-01 - Sin prefijo Bearer

Envía la petición preparada GET /productos. Usa encabezado `Authorization: {{tokenVendedor}}`, sin la palabra Bearer y con Authorization de Postman en No Auth para que no duplique el encabezado. Esperado **401**, `Debe iniciar sesión para acceder a este recurso`. Captura Headers del request y respuesta.

### T-02 - Payload alterado

La petición preparada crea tokenAlterado cambiando un carácter del segmento central y conservando header/firma originales. GET /productos con Bearer tokenAlterado debe devolver **401**, `El token no es válido`. Captura la preparación del token en el script y la respuesta. No reemplaza tokenVendedor.

### T-03 - Token firmado con otra clave

1. Conserva tokenVendedor de A-01. Detén el backend con Ctrl+C, no Oracle.
2. Genera otra clave de DESARROLLO y arranca con ella, en la misma terminal donde guardaste la configuración previa:

```powershell
$s10Bytes = New-Object byte[] 48
$s10Rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$s10Rng.GetBytes($s10Bytes)
$s10Rng.Dispose()
$env:JWT_SECRET = [Convert]::ToBase64String($s10Bytes)
./mvnw.cmd spring-boot:run
```

3. SIN volver a iniciar sesión, envía T-03 con tokenVendedor anterior. Esperado **401**, `El token no es válido`. Captura respuesta y el hecho de usar la variable del token anterior; no publiques la nueva clave.
4. Detén el backend y restaura el secreto original:

```powershell
if ($null -eq $s10SecretAnterior) {
    Remove-Item Env:JWT_SECRET -ErrorAction SilentlyContinue
} else {
    $env:JWT_SECRET = $s10SecretAnterior
}
$env:JWT_EXPIRACION_MINUTOS = '1'
./mvnw.cmd spring-boot:run
```

Ese arranque prepara T-04. No modificar V2 ni borrar cuentas para esta prueba.

### T-04 - Expiración real

1. Ejecuta login de T-04: debe responder 200 y `expiraEnSegundos: 60`; guarda tokenCorto. Captura.
2. GET inicial con ese token: **200**, captura.
3. Espera DOS MINUTOS completos sin renovar tokenCorto. Repite GET con el MISMO valor: **401**, `El token ha expirado. Inicie sesión nuevamente`. Captura. La colección no espera por ti.
4. Detén el backend, configura `$env:JWT_EXPIRACION_MINUTOS = '60'` y arranca otra vez. El secreto debe seguir restaurado.
5. Ejecuta la carpeta **Restauración · SECRETO Y DURACIÓN normales**: renueva admin, vendedor y almacen. Verifica 3600 segundos. Los tokens cortos anteriores no se alargan por reiniciar.

### T-05 - Claims del token normal

Con el nuevo tokenVendedor de 3600 segundos, usa la consola del navegador, sin enviar el JWT a webs externas:

```javascript
var tokenS10A = 'PEGA_TOKEN_VENDEDOR';
var payloadS10A = tokenS10A.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
payloadS10A = payloadS10A.padEnd(Math.ceil(payloadS10A.length / 4) * 4, '=');
var claimsS10A = JSON.parse(new TextDecoder().decode(
  Uint8Array.from(atob(payloadS10A), c => c.charCodeAt(0))
));
console.log(JSON.stringify(claimsS10A, null, 2));
console.log('Duración:', claimsS10A.exp - claimsS10A.iat);
```

Captura sub, authorities, iat, exp y duración **3600**. No debe haber password, hash o secreto en el payload. Decodificar no verifica la firma; esa comprobación es del servidor.

### T-06 propio - JWT malformado

GET /productos con `Authorization: Bearer abc.def.ghi`: **401**, `El token no es válido`. Es distinto de T-02: aquí el contenido no forma un JWT válido. Se eligió este caso propio para que no necesites otra herramienta de firma.

## Grupo P - permisos

Ejecuta después de restaurar clave y duración. Revisa siempre el token seleccionado, especialmente antes de DELETE.

| Caso | Operaciones | Esperado |
|---|---|---|
| P-01 | Vendedor: POST /categorias, PUT /categorias/9, DELETE /categorias/9 | Tres 403; captura cada método. No enviar estas peticiones con token admin. |
| P-02 | Almacén: GET /clientes y POST /ventas | Dos 403; no se registra venta. |
| P-03 | Vendedor y almacen: GET /reportes/productos-mas-vendidos | Dos 403. Opcional: captura @PreAuthorize del controlador como apoyo técnico. |
| P-04s | Admin: GET categorías, productos, clientes, buscar ventas, reporte, usuarios, empleados y roles | Ocho 200. Captura cada módulo; empleados/roles cargan automáticamente ids de prueba y roles. |
| P-05 propio | Vendedor: POST /usuarios | 403, sin alta. |

P-04 usa lecturas: no requiere registrar más ventas para probar el acceso de ADMIN.

## Grupo U - cuentas y persistencia

Orden de la colección: **U-01 → U-03 → U-02**. U-03 necesita al empleado todavía libre para que se compruebe el límite de bytes antes de existir una cuenta.

- **U-01a:** alta con password abcdefgh -> **400**, validationErrors.password por falta de número.
- **U-01b:** alta con rolIds [] -> **400**, validationErrors.rolIds.
- **U-03 propio:** password `a1` + 36 letras ñ; son 38 caracteres, pero 74 bytes UTF-8 -> **409**, `La contraseña no puede superar 72 bytes`. El cuerpo ya está en la colección.
- **U-02a:** alta con empleado 6, que ya tiene mhuaman -> **409**, empleado ya tiene usuario.
- **U-02b:** alta válida para el empleado separado, username **autonoma_s10**, password **Prueba2026***, rol VENDEDOR -> **201**. Captura el resultado sin password. Guarda automáticamente usuarioPruebaId.

La cuenta original de María se conserva. El empleado del fixture se llama Maria Huaman Pruebas S10; permite repetir la regla de alta del mismo modo y con la sustitución identificada. No se presenta la cuenta original existente como si acabara de crearse.

Consultas para U-02 (captura sus resultados):

```sql
SELECT id, username, empleado_id,
       SUBSTR(password, 1, 7) AS prefijo_bcrypt,
       LENGTH(password) AS longitud_hash
FROM usuarios WHERE username = 'autonoma_s10';

SELECT u.id, u.username, u.empleado_id, ur.rol_id, r.nombre
FROM usuarios u
JOIN usuario_roles ur ON ur.usuario_id = u.id
JOIN roles r ON r.id = ur.rol_id
WHERE u.username = 'autonoma_s10';
```

Esperado prefijo **$2a$10$**, longitud **60**, una fila de relación a VENDEDOR. No es necesario publicar todo el hash. Después ejecuta el login preparado de la cuenta de prueba y guarda tokenPrueba.

## C-01 - CORS desde la SPA

1. Arranca el frontend y abre `http://localhost:4200`. Sus 401 iniciales sin login no impiden hacer esta prueba; no hay que programar una pantalla nueva.
2. Abre F12 > Network, activa Preserve log. En consola de ESA página, pega un tokenVendedor actual:

```javascript
fetch('http://localhost:8080/api/v1/productos?corsS10=' + Date.now(), {
  headers: { Authorization: 'Bearer PEGA_TOKEN_VALIDO' }
}).then(async r => console.log('HTTP', r.status, await r.json()));
```

3. La URL nueva ayuda a evitar reutilizar la comprobación previa de CORS. En Network, captura **OPTIONS 200**: Origin localhost:4200 y Access-Control-Request-Headers con authorization; respuesta con Access-Control-Allow-Origin correcto.
4. Captura el GET real, Authorization y respuesta **200** (o 401 si el token venció), sin mensaje de bloqueo de CORS. Es una petición real del navegador, no un OPTIONS simulado en Postman.

## B-01 a B-04 - cambio de contraseña

Se usa SOLO autonoma_s10, no las cuentas habituales. Partimos de password **Prueba2026*** y tokenPrueba vigente.

| Caso | Peticiones y resultado |
|---|---|
| B-01 | Actual incorrecta + nueva válida -> **409**, La contraseña actual no es correcta. |
| B-02 | Actual y nueva Prueba2026* -> **409**, La nueva contraseña debe ser distinta de la actual. |
| B-03 | Nueva vacía, corta, sin número, sin letra -> **400** por passwordNueva. Variante UTF-8 >72 bytes -> **409**. Captura cada variante usada en la matriz. |
| B-04a | Actual Prueba2026*, nueva NuevaPrueba2026 -> **204**, cuerpo vacío. |
| B-04b | Login con Prueba2026* -> **401**. |
| B-04c | Login con NuevaPrueba2026 -> **200**, se guarda tokenPruebaNuevo, sin reemplazar tokenPrueba. |
| B-04d | GET /auth/me con tokenPrueba ANTERIOR al cambio -> **200**. No renovar ese token para esta comprobación. |

Antes de B-04a y después, ejecuta localmente la primera consulta de U-02 incluyendo password si quieres comparar los hashes; conserva la captura con prefijo/longitud para el informe. La prueba automatizada comprueba matches del nuevo hash. No deducir el texto de una contraseña a partir del prefijo BCrypt.

Captura también el log INFO `Contraseña actualizada: usuario=autonoma_s10`, sin contraseñas en el mensaje. Después de completar B-04 ejecuta **Restaurar password inicial** (actual NuevaPrueba2026, nueva Prueba2026*) -> 204. No repetir la restauración con una password que ya dejó de ser actual.

## B-05 y B-06 - roles

1. Renueva tokenPrueba ANTES de B-05. No vuelvas a hacer login entre sus peticiones.
2. GET del producto 134 captura sus valores actuales; la colección prepara productoActual.
3. PUT de ese producto como cuenta de prueba todavía VENDEDOR -> **403**.
4. Admin asigna [VENDEDOR, ALMACENERO] a usuarioPruebaId -> **200**, ambos roles en el DTO.
5. Repite PUT producto134 con el MISMO tokenPrueba -> **200**. Captura el token elegido, código y perfil de roles actualizado.
6. Admin reemplaza por SOLO ALMACENERO -> **200**. GET /clientes con ese MISMO tokenPrueba -> **403**: prueba que el conjunto se reemplaza y desaparecen los permisos de VENDEDOR.
7. Ejecuta la segunda consulta SQL de U-02 para mostrar las filas actuales de usuario_roles.
8. **Restaura VENDEDOR** con admin -> 200; captura y comprueba la fila SQL. El usuario no debe quedar con privilegios extra.
9. B-06: roles [] y [null] -> **400**; rol 999999 -> **404**; usuario 999999 con rol válido -> **404**. La colección separa los intentos.

El token conserva sus claims originales; los permisos actuales se obtienen de la cuenta recargada por username en JwtAuthenticationFilter. Esa es la explicación que acompañará B-05.

## B-07 - salvaguardas y autorización

- Como admin, intenta quitarte ADMIN asignando solo VENDEDOR a usuarioAdminId -> **409**, `No puede quitarse a sí mismo el rol ADMIN`.
- Como admin, intenta PATCH estado false para usuarioAdminId -> **409**, `No puede desactivarse a sí mismo`.
- Con tokenPrueba, intenta cambiar roles y estado -> **403** en ambos. Se restaura VENDEDOR antes de este grupo, como indica B-05.
- Guarda los cuatro resultados. No uses el id de otro administrador en las dos primeras peticiones: la regla es sobre la propia cuenta autenticada.

## B-08 - estado y tokens previos

1. Login autonoma_s10/Prueba2026* y conserva tokenPrueba ANTES de desactivar.
2. Admin PATCH estado false del usuario de prueba -> **200**, estado false.
3. GET /auth/me con ese tokenPrueba -> **401**, `El usuario está deshabilitado`.
4. Login con sus credenciales correctas -> **401**, mensaje genérico. Este intento no sobreescribe tokenPrueba.
5. **Reactivar** con tokenAdmin, PATCH estado true -> **200**.
6. GET /auth/me con el token previo todavía vigente -> **200**; login correcto -> **200**. La reactivación permite de nuevo la cuenta: los tokens no se guardan en una lista de revocación permanente.
7. Admin PATCH usuario 999999 -> **404**; estado null -> **400** por validationErrors.estado.
8. Conserva las capturas de todo el ciclo. Si el token de admin expiró, renueva SOLO admin para poder reactivar; no regeneres el token del usuario desactivado.

## Cierre y restauración

- Cuenta de prueba activa, solo VENDEDOR, password Prueba2026*. No borrar la cuenta: sirve de trazabilidad de la prueba.
- Cuenta original mhuaman y las cuentas habituales sin cambios.
- Secreto original y expiración normal restaurados. Al finalizar la sesión de validación puedes detener el backend, restaurar la variable de expiración previa y arrancar desde tu configuración habitual:

```powershell
if ($null -eq $s10ExpAnterior) {
    Remove-Item Env:JWT_EXPIRACION_MINUTOS -ErrorAction SilentlyContinue
} else {
    $env:JWT_EXPIRACION_MINUTOS = $s10ExpAnterior
}
```

- Exporta la colección final después de vaciar TODOS los valores de tokenAdmin, tokenVendedor, tokenAlmacen, tokenSupervisor, tokenPrueba, tokenPruebaNuevo, tokenCorto y tokenAlterado. Conserva las capturas locales antes de vaciar variables. El archivo del repositorio fue generado con esos tokens vacíos.
- Envía la carpeta de evidencia e indica cualquier resultado diferente. No marcar Pasa por anticipado. Se completará la matriz con código/mensaje realmente observados.
- Todavía no se generan el informe C1-C3 ni fichas de resultados manuales. Después de revisar las capturas, se prepararán informe de 2-3 páginas, copia para aula y documento separado con matriz y evidencias agrupadas, en el estilo acordado.
- No se han hecho commits; se mantienen para el final según la indicación actual.

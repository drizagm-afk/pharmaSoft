# S13: identificador de petición y logs

Fecha: 5 de noviembre de 2026. Segundo bloque del backend.

RequestIdFilter se ejecuta antes de Spring Security. Genera un identificador de ocho
caracteres hexadecimales, lo guarda en MDC.requestId y lo devuelve en X-Request-Id.
No acepta un identificador proporcionado por el cliente. CorsConfig expone la nueva
cabecera para que JavaScript pueda leerla en solicitudes entre orígenes permitidos.

Al terminar registra método, ruta sin query string, estado HTTP y duración en
milisegundos, medida con reloj monotónico. Una excepción propagada se registra con
estado 500; el filtro no oculta la excepción. El contexto MDC se elimina en finally,
incluso si falla el procesamiento de la petición. Las llamadas a /actuator y sus
subrutas reciben identificador, pero se omite su log de acceso para evitar ruido.

## Perfiles

- Dev: patrón con hora, nivel, [requestId], logger y mensaje. Hibernate show-sql se
  desactiva y org.hibernate.SQL usa DEBUG para emitir el SQL mediante el logger,
  conservando el identificador de la petición en lugar de imprimir fuera del patrón.
- Prod: consola y logs/pharma-backend.log incluyen [requestId]. Rotación de 10 MB
  y max-history=14. Se mantiene show-sql=false; el logger SQL queda en WARN y los
  valores de parámetros JDBC en OFF. La carpeta logs/ queda ignorada por Git.

El filtro no registra Authorization, cookies, contraseñas, cuerpos ni parámetros
de consulta. Los logs existentes de autenticación no incluyen el token ni la clave.
La prueba de valores sensibles usa textos ficticios y verifica que no aparecen en
el mensaje de acceso.

## Verificación automatizada

Resultado: 80 casos contabilizados, 70 aprobados, cero fallos/errores y 10 omitidos
que requieren integración Oracle habilitada expresamente.

- RequestIdFilterTest contrasta cabecera, MDC y log para estados 200, 401 y 403;
  comprueba IDs distintos en peticiones consecutivas y limpieza del contexto.
- Comprueba propagación de excepciones, registro 500 y limpieza posterior.
- Comprueba exclusión de datos sensibles y de logs de acceso de Actuator.
- SeguridadIntegrationTest verifica el filtro en la cadena real de Spring Security:
  salud pública, rechazo anónimo 401, rechazo por permisos 403 y exposición CORS.
- La regresión conserva los seis casos de Redis real y el fallback con conexión
  Redis inaccesible. Las bases de datos utilizadas son H2, no Oracle.

Ejecución: target/s13-logs-tests.log, ignorado por Git. Los patrones y la rotación de
producción están configurados; su comprobación manual con el perfil prod y las
capturas se realizará en la fase de despliegue. No se hicieron commits automáticos.

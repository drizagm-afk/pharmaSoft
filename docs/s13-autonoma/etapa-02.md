# Etapa 2 — Identificador en respuestas de error

## Comportamiento

ErrorResponseDTO incluye requestId. GlobalExceptionHandler toma su valor del MDC
mediante la misma clave que utiliza RequestIdFilter. Las respuestas de seguridad
generadas fuera del controlador también incluyen el campo, tanto en rechazos 401
como 403. No se genera un ID nuevo en el manejador de errores: se conserva el de
la petición, que ya aparece en el encabezado X-Request-Id y en los logs.

Se conservan los códigos HTTP, mensajes y validationErrors existentes, así como las
modificaciones previas del manejador de excepciones. Si el contexto no contiene un
ID, el campo se representa como null; no se inventa una correlación.

## Comprobación técnica

Comando ejecutado con Java 25 y las dependencias locales de Maven:

```text
./mvnw.cmd -o -Dtest=SeguridadIntegrationTest,RequestIdFilterTest test
```

Resultado: 56 pruebas aprobadas, cero fallos, cero errores y cero omisiones.
La suite utiliza H2 aislado y transacciones revertidas; no modifica Oracle.

La nueva comprobación recorre peticiones reales a través de MockMvc y la cadena de
seguridad, incluyendo recurso sin autenticar, métricas sin permiso, producto inexistente,
validación de categoría, JSON malformado, regla de negocio y credenciales incorrectas.
Para cada una verifica el código esperado (400, 401, 403, 404 o 409), el mensaje,
la igualdad requestId del cuerpo/encabezado, el formato de ocho caracteres, un ID
distinto entre peticiones y la limpieza del MDC al finalizar.

El resultado técnico está en target/s13-autonoma-errors-tests.log. Las capturas
manuales de la actividad se realizarán en su fase de evidencias.

## Siguiente etapa

Ampliar el modelo de error de la SPA y mostrar Código de seguimiento cuando la
respuesta incluya requestId. Esta etapa implementa únicamente el backend.

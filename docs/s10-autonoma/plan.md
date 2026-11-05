# S10 autónoma - contexto compacto y plan propuesto

Estado: implementación, validación manual confirmada y documentos finales terminados. Informe de tres páginas, matriz de 28 casos y documento de evidencias agrupadas revisados. Sin commits por instrucción del usuario.

## Contexto que se conserva

- Backend en `feature/seguridad-jwt`, último commit observado d3c50fc. Seguridad práctica implementada: BCrypt, JWT, roles/permisos, /auth/login, /auth/me y administración de altas/listados; Flyway V2 aplicado a Oracle sin borrar datos comerciales.
- Última regresión de implementación: 50 pruebas aprobadas, incluidas comprobaciones Oracle. No se están volviendo a ejecutar ni modificando datos para preparar este plan.
- La validación manual posterior registró ventas 54 y 55 y creó mhuaman (usuario 21, empleado 6, VENDEDOR 2). Son resultados históricos, no un inventario vivo; revisar la base antes de preparar nuevas pruebas.
- Informe práctico existente en docs/DelCarpio_LP2_S10_Practica.pdf; evidencias originales copiadas en docs/s10/evidencias con hashes y metadatos. No sobrescribirlo con la actividad autónoma.
- El usuario realiza los commits. No publicar PR ni subir al aula sin instrucción específica. Primero aprobar plan, después construir y verificar automáticamente, luego preparar guía manual, después revisar capturas y elaborar documentos finales.
- Estilo documental: párrafos negros, encabezados azules y separadores dorados; ejemplos inline; resto de evidencia agrupada por aspecto, título y descripción. Sin índice de evidencias, nombres de archivos en los títulos, conversación, términos como bundle o referencias en tercera persona al estudiante. Mantener resultados fieles a la evidencia.

## Requisitos revisados

Fuente: actividad autónoma S10, siete páginas, revisión textual completa y segunda inspección de requisitos/formato.

- Solo backend. Fecha límite 21/10/2026 23:59, America/Lima; 20 puntos.
- A (8 puntos): matriz de ocho columnas: ID, Grupo, Usuario/token, Petición, Resultado esperado (código y mensaje), Resultado obtenido, Estado, Evidencia. 16 casos base: A01-A04, T01-T05, P01-P04, U01-U02 y C01. Cuatro propios de grupos diferentes, más ocho B01-B08: mínimo 28 filas, algunas con varias peticiones.
- B (7 puntos): tres endpoints nuevos. La introducción menciona dos, pero B1-B3 y el producto esperado enumeran tres; seguir las tres especificaciones.
- C (3 puntos): informe principal de 2-3 páginas con C1 conteos por grupo, C2 fichas por fallos reales y C3 cuatro párrafos de análisis con referencias al código. Evidencias completas en documento separado para respetar la extensión principal.
- D (2 puntos): nueva rama desde la práctica, tres commits y PR hacia master. El PDF solicita días distintos; se conserva la excepción comunicada previamente por el usuario de que el profesor permite commits el mismo día. No cambiar fechas ni fabricar commits.
- Salidas: docs/matriz-seguridad-s10.md (o xlsx), docs/informe-s10.pdf, postman/PharmaSoft-S10-Seguridad.json sin tokens reales; copia de entrega DelCarpio_LP2_S10_Autonoma.pdf.

## Construcción propuesta

1. PUT /api/v1/auth/password, cualquier cuenta autenticada. DTO CambioPasswordRequestDTO; identidad desde Authentication, nunca desde un id del Body. passwordNueva: 8-72 caracteres, letra y número, y límite BCrypt de 72 bytes UTF-8. Actual incorrecta -> 409; nueva igual -> 409; válida -> hash nuevo, log de usuario sin contraseñas y 204. El JWT anterior sigue válido hasta expirar, según B1 y C3; no añadir revocación por cambio de password en este alcance.
2. PUT /api/v1/usuarios/{id}/roles, USUARIOS_ADMINISTRAR. DTO AsignarRolesRequestDTO con conjunto no vacío de ids no nulos. Reemplazar roles en transacción; usuario/rol inexistente -> 404; autoeliminación de ADMIN -> 409; devolver perfil 200. Comprobar con el mismo token la autorización actualizada por recarga de usuario en JwtAuthenticationFilter.
3. PATCH /api/v1/usuarios/{id}/estado, USUARIOS_ADMINISTRAR. DTO de estado obligatorio. Usuario inexistente -> 404; autodesactivación -> 409; devolver perfil 200. Desactivado: token previo y login dejan de autenticar. Permitir reactivación por otro administrador autorizado.
4. Reutilizar entidades y tablas existentes: no se prevé nueva migración. Mantener arquitectura Controller -> Service -> Repository y contrato de errores actual.

## Casos propios propuestos

- A05 (Autenticación): login válido con espacios exteriores en username; regla esperada trim + búsqueda sin distinguir mayúsculas.
- T06 (Token): JWT malformado abc.def.ghi -> 401; complemento de las pruebas de firma, expiración y ausencia de token.
- P05 (Permisos): VENDEDOR intenta POST /usuarios -> 403, sin alta.
- U03 (Usuarios): contraseña con caracteres multibyte que supera 72 bytes -> 409 y sin cuenta creada.

La numeración B01-B08 se solicita, pero sus ocho definiciones no aparecen en el PDF. Proponer esta distribución y registrar subverificaciones cuando una fila cubre varias reglas:

| ID | Alcance |
|---|---|
| B01 | Password actual incorrecta: 409, hash sin cambios |
| B02 | Password nueva igual: 409 |
| B03 | Password nueva inválida: 400; variantes de formato y límite UTF-8 |
| B04 | Cambio válido: 204, hash BCrypt, login anterior 401 y nuevo 200, JWT previo todavía válido |
| B05 | Reemplazo de roles 200, filas de unión correctas y nuevo permiso con mismo token |
| B06 | Roles vacíos 400; usuario o rol inexistente 404 |
| B07 | Salvaguardas del administrador: autoquitar ADMIN y autodesactivar 409; otras cuentas sin permiso 403 |
| B08 | Estado: desactivar/reactivar 200, token previo/login 401 mientras inactivo; usuario inexistente 404 y estado inválido 400 |

## Secuencia y tiempos orientativos

| Fase | Trabajo | Tiempo | Commit sugerido |
|---|---|---|---|
| 1 | Inspección final, rama y fixtures aislados para automatización | 10-15 min | Junto con la primera implementación |
| 2 | DTO/servicio/controlador de password y pruebas | 30-45 min | feat(auth): cambio de contraseña del usuario autenticado |
| 3 | Roles, estado, salvaguardas y pruebas | 40-60 min | feat(usuarios): asignar roles y activar o desactivar usuarios |
| 4 | Regresión, matriz preliminar, colección y guía manual | 25-40 min | test(seguridad): matriz y colección de autenticación y permisos |
| 5 | Ejecución manual y capturas por el usuario | 60-90 min | No marcar resultados antes de ejecutar |
| 6 | Revisión de evidencia, informe 2-3 páginas y documento de evidencia agrupada | 30-45 min | docs(seguridad): informe y evidencias S10 |

## Preparación de evidencias

- Preservar mhuaman y cuentas existentes. U02 necesita una alta válida: planificar un empleado de prueba separado y documentar los datos equivalentes, o reutilizar evidencia histórica claramente identificada; no borrar la cuenta de María para repetirla.
- Evitar que las automatizadas consuman las precondiciones de la validación manual: fixtures aislados y rollback/limpieza de datos propios. Resultados manuales de matriz quedan pendientes.
- Cambios manuales de password, roles, estado, secreto y expiración se harán con secuencia explícita de restauración. Guardar tokens previos antes de cambiar roles/estado y no renovarlos al probar su efecto inmediato.
- CORS C01 se puede ejecutar desde la consola de la SPA en localhost:4200 con fetch y Authorization para provocar OPTIONS; no requiere construir una pantalla de Angular.
- Capturas con método, URL, Authorization, código y respuesta; subcapturas para casos de varias operaciones. Tokens guardados solo localmente; limpiar variables antes de exportar la colección final.
- C3: mensaje uniforme de login; por qué persiste el JWT tras cambiar password y forma propuesta de invalidarlo; compromiso/rotación de secreto; ventajas y desventajas de @PreAuthorize. Además, explicar la actualización de permisos con el mismo token exigida por B2.

## Resultado de implementación

- Rama feature/seguridad-pruebas-delcarpio; no se realizaron commits.
- Tres endpoints implementados con validación, transacciones y salvaguardas.
- Maven test: 57 pruebas ejecutadas y aprobadas; 10 pruebas Oracle optativas omitidas. H2 aislado para seguridad; sin cambios en Oracle durante esta implementación.
- Maven package -DskipTests: BUILD SUCCESS después de la revisión de imports.
- Colección verificada: 79 peticiones, 93 scripts con sintaxis válida y ocho variables de token vacías. Cuerpos JSON comprobados con sustituciones de prueba.
- Matriz de 28 casos con resultados manuales pendientes; guía en guia-evidencias.md y fixture SQL preparado sin ejecutar.
- Próxima fase: reiniciar backend, preparar empleado de prueba, ejecutar manualmente y capturar evidencias siguiendo la guía. Después se revisarán resultados y se generarán los documentos finales.

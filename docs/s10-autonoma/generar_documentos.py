"""Build the security report and grouped evidence, preserving original image pixels."""
from pathlib import Path
from datetime import datetime, timezone, timedelta
from html import escape
import hashlib, json, shutil
from PIL import Image as PILImage
from pypdf import PdfReader
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, Image, PageBreak, KeepTogether
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4, landscape
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont

ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'docs'; WORK=OUT/'s10-autonoma'; DEST=WORK/'evidencias'; DEST.mkdir(exist_ok=True)
NAVY=colors.HexColor('#12325b'); GOLD=colors.HexColor('#eda900')
for name,file in [('Arial','arial.ttf'),('ArialBold','arialbd.ttf')]:
 pdfmetrics.registerFont(TTFont(name,str(Path('C:/Windows/Fonts')/file)))
pdfmetrics.registerFontFamily('Arial',normal='Arial',bold='ArialBold',italic='Arial',boldItalic='ArialBold')
def st(size=9.5,bold=False,color=colors.black):
 return ParagraphStyle('text',fontName='ArialBold' if bold else 'Arial',fontSize=size,leading=size+3,spaceAfter=6,textColor=color)
BODY=st(); SMALL=st(8); H1=st(17,True,NAVY); H2=st(12,True,NAVY)
def para(t,s=BODY): return Paragraph(t,s)
def fig(path,width,caption=None):
 with PILImage.open(path) as im: w,h=im.size
 image=Image(str(path),width=width,height=width*h/w); image.hAlign='CENTER'
 return KeepTogether([image,Spacer(1,5)]+([para(caption,SMALL)] if caption else []))
def table(rows,widths,size=8):
 cells=[[para(escape(str(v)),st(size,i==0,colors.white if i==0 else colors.black)) for v in row] for i,row in enumerate(rows)]
 t=Table(cells,colWidths=widths,repeatRows=1,hAlign='LEFT')
 t.setStyle(TableStyle([('BACKGROUND',(0,0),(-1,0),NAVY),('ROWBACKGROUNDS',(0,1),(-1,-1),[colors.white,colors.HexColor('#f1f4f8')]),('GRID',(0,0),(-1,-1),.3,colors.HexColor('#bdc9d8')),('VALIGN',(0,0),(-1,-1),'TOP'),('LEFTPADDING',(0,0),(-1,-1),5),('RIGHTPADDING',(0,0),(-1,-1),5),('TOPPADDING',(0,0),(-1,-1),3),('BOTTOMPADDING',(0,0),(-1,-1),3)]))
 return t
def frame(label):
 def draw(c,d):
  w,h=d.pagesize
  c.setFillColor(NAVY);c.setFont('ArialBold',8);c.drawString(38,h-25,'LP II · Sesión 10 · '+label)
  c.setStrokeColor(GOLD);c.setLineWidth(1.5);c.line(38,h-32,w-38,h-32)
  c.setFillColor(colors.black);c.setFont('Arial',7);c.drawString(38,20,'Kevin Eduardo Del Carpio Alegría · 18 de octubre de 2026');c.drawRightString(w-38,20,str(d.page))
 return draw
def build(path,story,size=A4):
 logo=ROOT.parent/'(IV - Class) PharmaFrontend/docs/s8/assets/upeu-logo.png'
 with PILImage.open(logo) as im: lw,lh=im.size
 emblem=Image(str(logo),width=110,height=110*lh/lw);emblem.hAlign='CENTER'
 story=[emblem,Spacer(1,10)]+story
 doc=SimpleDocTemplate(str(path),pagesize=size,leftMargin=38,rightMargin=38,topMargin=45,bottomMargin=36,title='PharmaSoft - Seguridad S10',author='Kevin Eduardo Del Carpio Alegría')
 doc.build(story,onFirstPage=frame('Seguridad backend'),onLaterPages=frame('Seguridad backend'))

# Captures indexed in creation-time order. Prepared requests and repeated checks remain identified.
RAW=[
('A-01','Login de administrador','200; ADMIN y sus permisos.'),
('A-01','Login de vendedor','200; VENDEDOR y permisos comerciales.'),
('A-01','Preparación del login de supervisor','Solicitud preparada para comprobar la unión de roles.'),
('A-01','Login de supervisor','200; ALMACENERO y VENDEDOR con la unión de permisos.'),
('AUX','Perfil del administrador','200; perfil actual y permisos administrativos.'),
('A-02','Usuario inexistente','401; Usuario o contraseña incorrectos.'),
('A-03','Cuenta inactiva','401; el mensaje no revela la existencia de la cuenta.'),
('A-05','Username con espacios exteriores','200; el login normaliza los espacios del username.'),
('T-01','Token sin prefijo Bearer','401; Debe iniciar sesión para acceder a este recurso.'),
('T-02','Payload alterado','401; El token no es válido.'),
('T-02','Payload alterado: repetición','Se confirma el mismo rechazo 401 con el token alterado.'),
('T-03','Acceso con el secreto original','200 antes de cambiar la clave de firma.'),
('T-03','Token anterior con otro secreto','401; El token no es válido después del cambio de secreto.'),
('T-04','Emisión de token corto','200; expiraEnSegundos igual a 60.'),
('T-04','Acceso antes del vencimiento','200 con el token de un minuto todavía vigente.'),
('T-04','Acceso después del vencimiento','401; El token ha expirado. Inicie sesión nuevamente.'),
('T-05','Claims del JWT decodificados localmente','sub vendedor, authorities, iat y exp; en esta emisión corta, exp - iat = 60 segundos.'),
('T-06','JWT malformado','401; El token no es válido con abc.def.ghi.'),
('REST','Renovación de administrador','200; autenticación después de restaurar la configuración normal.'),
('REST','Renovación de vendedor','200; nueva credencial para los casos posteriores.'),
('REST','Renovación de almacén','200; ALMACENERO y permisos del catálogo.'),
('P-01','POST categorías como vendedor','403; No tiene permisos para realizar esta operación.'),
('P-01','PUT categorías como vendedor','403; actualización rechazada por falta de permiso.'),
('P-01','DELETE categorías como vendedor','403; eliminación rechazada por falta de permiso.'),
('P-01','PUT categorías: comprobación adicional','403; se mantiene el rechazo al vendedor.'),
('P-01','DELETE categorías: comprobación adicional','403; se confirma la protección de la operación.'),
('P-02','Lectura de clientes desde almacén','403; ALMACENERO no tiene CLIENTES_LEER.'),
('P-02','Clientes desde almacén: repetición','403; se confirma el límite de permisos.'),
('P-02','Venta desde almacén','403; no se autoriza registrar ventas.'),
('P-03','Reporte con cuenta de almacén','403; el reporte exige REPORTES_VER.'),
('P-03','Reporte con vendedor','403; rechazo de autorización del reporte.'),
('P-03','Reporte con almacén: repetición','403; la cuenta autenticada carece de REPORTES_VER.'),
('P-04','Administrador consulta categorías','200; listado paginado del módulo.'),
('P-04','Administrador consulta productos','200; listado del catálogo.'),
('P-04','Administrador consulta clientes','200; acceso al módulo de clientes.'),
('P-04','Administrador consulta ventas','200; acceso al historial de ventas.'),
('P-04','Administrador consulta reportes','200; reporte de ventas por categoría.'),
('P-04','Administrador consulta usuarios','200; perfiles sin campo password.'),
('P-04','Administrador consulta empleados','200; datos y disponibilidad de cuentas.'),
('P-04','Administrador consulta roles','200; roles y permisos disponibles.'),
('P-05','Vendedor intenta crear usuarios','403; solo el permiso administrativo autoriza el alta.'),
('U-01','Contraseña sin números','400; validationErrors.password informa la regla de letra y número.'),
('U-01','Alta sin roles','400; validationErrors.rolIds exige al menos un rol.'),
('U-03','Contraseña multibyte extensa','409; La contraseña no puede superar 72 bytes.'),
('U-02','Empleado con cuenta existente','409; se protege la relación uno a uno entre empleado y usuario.'),
('U-02','Creación de la cuenta de prueba','201; autonoma_s10, usuario 22, empleado 21, VENDEDOR, sin password en la respuesta.'),
('AUX','Login de la cuenta de prueba','200; token de autonoma_s10 para las operaciones de la parte B.'),
('B-01','Contraseña actual incorrecta','409; La contraseña actual no es correcta.'),
('B-02','Nueva contraseña igual a la actual','409; La nueva contraseña debe ser distinta de la actual.'),
('B-03','Nueva contraseña vacía','400; validación de passwordNueva.'),
('B-03','Nueva contraseña corta','400; tamaño mínimo y reglas de complejidad.'),
('B-03','Nueva contraseña sin números','400; debe incluir al menos una letra y un número.'),
('B-03','Nueva contraseña sin letras','400; debe incluir al menos una letra y un número.'),
('B-03','Nueva contraseña excede 72 bytes','409; se aplica el límite de entrada de BCrypt en UTF-8.'),
('B-04','Cambio de contraseña válido','204 No Content; operación sobre la identidad autenticada.'),
('B-04','Login con la contraseña anterior','401; la contraseña anterior deja de autenticar.'),
('B-04','Login con la contraseña nueva','200; la nueva contraseña autentica correctamente.'),
('B-04','JWT anterior al cambio sigue válido','200 en auth/me con el token emitido antes del cambio.'),
('REST','Restauración de contraseña','204; se restablece la contraseña inicial de la cuenta de prueba.'),
('PREP','Preparación del entorno del navegador','Intento inicial con localhost sin el frontend iniciado; se repite correctamente desde la SPA en C-01.'),
('B-05','Sin permiso de edición antes del cambio','403; la cuenta VENDEDOR no puede editar productos.'),
('B-05','Asignación de dos roles','200; respuesta con ALMACENERO y VENDEDOR y sus permisos.'),
('B-05','Edición con el mismo token','200; el permiso nuevo se aplica sin un nuevo login.'),
('B-05','Reemplazo por solo almacén','200; el conjunto de roles queda únicamente en ALMACENERO.'),
('B-05','Pérdida de lectura de clientes','403 con el mismo token tras retirar VENDEDOR.'),
('REST','Restauración de VENDEDOR','200; se restablece el rol inicial de la cuenta de prueba.'),
('B-06','Conjunto de roles vacío','400; rolIds debe contener al menos un rol.'),
('B-06','Elemento nulo en roles','400; validación del elemento de rolIds.'),
('B-06','Rol inexistente','404; Roles no encontrados: [999999].'),
('B-06','Usuario inexistente al asignar roles','404; Usuario no encontrado con id: 999999.'),
('B-07','Administrador intenta quitarse ADMIN','409; No puede quitarse a sí mismo el rol ADMIN.'),
('B-07','Autoeliminación de ADMIN: autorización','Vista del token administrador y el mismo rechazo 409.'),
('B-07','Administrador intenta autodesactivarse','409; No puede desactivarse a sí mismo.'),
('B-07','Autodesactivación: autorización','Vista del token administrador y el rechazo 409.'),
('B-07','No administrador intenta cambiar roles','403; falta USUARIOS_ADMINISTRAR.'),
('B-07','Cambio de roles no autorizado: token','Se confirma 403 con el token de la cuenta de prueba.'),
('B-07','No administrador intenta cambiar estado','403; falta USUARIOS_ADMINISTRAR.'),
('B-07','Cambio de estado no autorizado: token','Se confirma el rechazo 403 a la cuenta no administradora.'),
('B-08','Token previo a desactivar','200; login y emisión del token que se conserva durante la prueba.'),
('B-08','Desactivación de la cuenta','200; estado false en el perfil de autonoma_s10.'),
('B-08','Acceso con token de cuenta desactivada','401; El usuario está deshabilitado.'),
('B-08','Login de cuenta desactivada','401; Usuario o contraseña incorrectos.'),
('B-08','Reactivación de la cuenta','200; estado true.'),
('B-08','Token previo después de reactivar','200; vuelve a funcionar mientras conserva firma y vigencia.'),
('B-08','Login después de reactivar','200; autenticación nuevamente permitida.'),
('B-08','Estado de un usuario inexistente','404; Usuario no encontrado con id: 999999.'),
('B-08','Estado nulo','400; validationErrors.estado indica que el estado es obligatorio.'),
]
files=sorted(Path('C:/Users/DRIZA/Documents/UNI/Evidence').glob('*.png'),key=lambda f:(f.stat().st_birthtime,f.name))
assert len(files)==len(RAW)==87
extra=Path('C:/Users/DRIZA/OneDrive/Imágenes/Capturas de pantalla')
for stamp,title,desc in [
 ('102548','Consulta desde la SPA','Consola de localhost:4200: HTTP 200 y JSON de productos.'),
 ('102554','GET autorizado por CORS','GET 200; Access-Control-Allow-Origin igual a http://localhost:4200.'),
 ('102606','Respuesta JSON accesible','El navegador muestra el contenido de productos en Response.'),
 ('102627','Preflight OPTIONS','OPTIONS 200; origen localhost:4200, authorization permitido y métodos autorizados.'),
 ('102635','Cabeceras completas del preflight','HTTP 200; Allow-Origin, Allow-Methods, Allow-Headers y Max-Age en vista Raw.')]:
 files.append(extra/f'Captura de pantalla 2026-10-18 {stamp}.png');RAW.append(('C-01',title,desc))
manifest=[]
for n,(f,(case,title,desc)) in enumerate(zip(files,RAW),1):
 target=DEST/f'{case}_{n:03}.png';shutil.copy2(f,target)
 manifest.append(dict(numero=n,caso=case,titulo=title,descripcion=desc,archivo=target.name,original=str(f),creado=datetime.fromtimestamp(f.stat().st_birthtime,timezone(timedelta(hours=-5))).isoformat(),sha256=hashlib.sha256(f.read_bytes()).hexdigest()))
(WORK/'manifest-evidencias.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding='utf-8')
def evidence(n,width,caption=None):return fig(DEST/manifest[n-1]['archivo'],width,caption)

# Keep the matrix's required eight columns and record the basis for each obtained result.
matrix=OUT/'matriz-seguridad-s10.md'
rows=[]
for line in matrix.read_text(encoding='utf-8').splitlines():
 if line.startswith('| ') and not line.startswith('| ---'):
  fields=[x.strip() for x in line.strip('|').split('|')]
  if fields[0]!='ID':rows.append(fields)
assert len(rows)==28
observed={
'A-01':'200; ADMIN, VENDEDOR y supervisor con unión de roles. Login ALMACENERO 200 en renovación.',
'A-02':'401: Usuario o contraseña incorrectos; mensaje uniforme confirmado en la ejecución y en credencialesInvalidasMismoMensaje.',
'A-03':'401: Usuario o contraseña incorrectos.',
'A-04':'200; username canónico vendedor. Ejecución confirmada y prueba loginYPerfilSinPassword con VENDEDOR.',
'A-05':'200; espacios exteriores eliminados del username.',
'T-01':'401: Debe iniciar sesión para acceder a este recurso.',
'T-02':'401: El token no es válido.',
'T-03':'200 con clave original y 401: El token no es válido tras cambiarla.',
'T-04':'200 al emitir/acceder; 401: El token ha expirado. Inicie sesión nuevamente después del vencimiento.',
'T-05':'sub vendedor, authorities, iat y exp; emisión corta con duración 60 s, sin password ni hash.',
'T-06':'401: El token no es válido.',
'P-01':'403 en POST, PUT y DELETE: No tiene permisos para realizar esta operación.',
'P-02':'403 en GET clientes y POST ventas: No tiene permisos para realizar esta operación.',
'P-03':'403 para vendedor y almacén: No tiene permisos para realizar esta operación.',
'P-04':'200 en las ocho consultas de módulos y administración.',
'P-05':'403: No tiene permisos para realizar esta operación.',
'U-01':'400 en ambas variantes; validationErrors de password y rolIds.',
'U-02':'409 para empleado ocupado; 201 para autonoma_s10 (usuario 22, empleado 21), sin password. BCrypt y relaciones comprobados además en pruebas aisladas.',
'U-03':'409: La contraseña no puede superar 72 bytes.',
'C-01':'OPTIONS 200; GET 200 con JSON y Allow-Origin http://localhost:4200; sin bloqueo CORS.',
'B-01':'409: La contraseña actual no es correcta. Hash intacto comprobado en passwordActualIncorrectaNoModificaHash.',
'B-02':'409: La nueva contraseña debe ser distinta de la actual.',
'B-03':'400: validationErrors.passwordNueva en cuatro formatos; 409: La contraseña no puede superar 72 bytes.',
'B-04':'204; login anterior 401 y nuevo 200; JWT anterior 200. Hash BCrypt comprobado en passwordNuevaBcryptYTokenPrevioPermaneceValido.',
'B-05':'403 antes, asignación 200, edición 200 con mismo JWT; reemplazo 200 y clientes 403; restauración 200. Relación JPA cubierta por pruebas aisladas.',
'B-06':'400 con roles vacíos/nulos; 404: Roles no encontrados: [999999] y Usuario no encontrado con id: 999999.',
'B-07':'409: No puede quitarse a sí mismo el rol ADMIN / No puede desactivarse a sí mismo. No admin: 403 en roles y estado.',
'B-08':'Desactivar/reactivar 200; token previo 401: El usuario está deshabilitado; login 401 genérico; reactivación permite token previo y login 200; inexistente 404 y estado nulo 400.'}
for row in rows:
 case=row[0];row[5]=observed[case];row[6]='Pasa'
 nums=[m['numero'] for m in manifest if m['caso']==case]
 row[7]=', '.join('E'+str(n).zfill(3) for n in nums) if nums else 'Confirmación de ejecución; loginYPerfilSinPassword (automatizada)'
head=['ID','Grupo','Usuario / token','Petición','Resultado esperado (código y mensaje)','Resultado obtenido','Estado','Evidencia']
matrix.write_text('# Matriz de seguridad S10 - resultados consolidados\n\n28 casos: 16 base, cuatro propios de grupos distintos y ocho B. Ejecución manual del 18/10/2026 confirmada; las referencias E identifican las figuras del documento de evidencias. Las comprobaciones automatizadas complementarias se identifican expresamente y usan H2 aislado, no una consulta a Oracle. T-05 utiliza el token corto de 60 segundos observado.\n\n| '+' | '.join(head)+' |\n| '+' | '.join(['---']*8)+' |\n'+'\n'.join('| '+' | '.join(r)+' |' for r in rows)+'\n',encoding='utf-8')

# Main report: exactly three pages, with inline evidence and four distinct C3 paragraphs.
story=[para('Informe de seguridad',H1),para('PharmaBackend · Actividad autónoma · Sesión 10',H2),para('Kevin Eduardo Del Carpio Alegría · Lenguaje de Programación II · Ciclo IV · 2026-2',SMALL),para('C1. Resumen de verificación',H2),para('Se ejecutaron 28 casos de autenticación, JWT, permisos, usuarios, CORS y administración de credenciales. Los rechazos 400, 401, 403, 404 y 409 previstos son resultados correctos: cada estado se compara con la regla del caso, no con la expectativa de obtener siempre 200.')]
story.append(table([['Grupo','Ejecutados','Pasa','Falla'],['Autenticación (A)','5','5','0'],['Token (T)','6','6','0'],['Permisos (P)','5','5','0'],['Usuarios (U)','3','3','0'],['CORS (C)','1','1','0'],['Contraseña, roles y estado (B)','8','8','0'],['Total','28','28','0']],[265,76,76,64]))
story.extend([para('La regla de username acepta mayúsculas y elimina espacios exteriores: AuthServiceImpl.login aplica trim y el repositorio busca sin distinguir mayúsculas. Los cuatro casos propios son A-05, T-06, P-05 y U-03. La cuenta autonoma_s10 (usuario 22, empleado 21, Maria Huaman Pruebas S10) permite probar la parte B conservando la cuenta previa de María.'),evidence(16,440,'Figura 1. T-04: el token corto vencido recibe 401 con un mensaje específico de expiración.'),para('La matriz consolida los resultados confirmados de ejecución. Las capturas documentan las respuestas HTTP; las comprobaciones complementarias de hashes y relaciones se identifican como pruebas automatizadas en H2. La regresión ejecutó 57 pruebas sin fallos; otras 10 pruebas Oracle optativas no se ejecutaron.',SMALL),PageBreak()])
story.extend([para('Implementación y comportamiento comprobado',H1),para('<b>Contraseña.</b> AuthController.cambiarPassword obtiene el username de Authentication. AuthServiceImpl.cambiarPassword verifica la actual con PasswordEncoder.matches, rechaza la repetición y codifica la nueva con BCrypt. CambioPasswordRequestDTO exige entre 8 y 72 caracteres, letra y número; el servicio también controla 72 bytes UTF-8. B-01 a B-04 obtienen 409, 400 y 204 según la regla. El login antiguo pasa a 401, el nuevo a 200 y el JWT anterior conserva su validez. El log INFO del servicio registra el username sin incluir contraseñas.'),para('<b>Roles.</b> UsuarioServiceImpl.asignarRoles carga y valida todos los ids y reemplaza el conjunto mediante setRoles dentro de una transacción. B-05 muestra 403 antes del cambio, 200 al asignar VENDEDOR y ALMACENERO y 200 al editar con el mismo token. Al reemplazar por solo ALMACENERO, clientes vuelve a 403. B-06 confirma 400 para conjuntos inválidos y 404 para ids inexistentes.'),evidence(62,440,'Figura 2. B-05: respuesta 200 con ambos roles y sus permisos efectivos.'),para('<b>Permisos actuales con el mismo JWT.</b> JwtAuthenticationFilter.doFilterInternal valida el token, extrae username y llama a loadUserByUsername en cada petición. Construye Authentication con las autoridades actuales de la cuenta, no con una copia fija de los claims authorities. Por eso el cambio de roles aplica en la siguiente petición sin repetir el login.'),para('<b>Estado y salvaguardas.</b> UsuarioServiceImpl.cambiarEstado conserva la cuenta y su historial. B-08 muestra 200 al desactivar, 401 con el token previo y 401 genérico en login; al reactivar, ambos accesos vuelven a 200 si el JWT sigue vigente. B-07 confirma 409 al autoquitar ADMIN o autodesactivarse; una cuenta sin USUARIOS_ADMINISTRAR recibe 403.'),para('C2. Hallazgos',H2),para('No se registraron casos funcionales fallidos en la ejecución consolidada: 28 pasan y 0 fallan. Por ello no corresponden fichas de defecto con severidad, causa probable y corrección. El intento inicial de CORS se repitió con la SPA iniciada y terminó con OPTIONS 200 y GET 200; fue una condición del entorno, no un defecto del backend.'),PageBreak()])
story.extend([para('C3. Análisis de seguridad',H1),para('<b>1. Mensaje uniforme de login.</b> El usuario inexistente, la contraseña incorrecta y la cuenta deshabilitada reciben 401 con el mismo mensaje. Esto evita que un atacante use la respuesta para enumerar cuentas o conocer su estado. El manejo de errores de autenticación mantiene el contrato genérico, mientras JwtAuthenticationFilter distingue la deshabilitación al evaluar un JWT ya emitido; se separa así la información necesaria de una petición autenticada del diagnóstico público del login.'),para('<b>2. Token después del cambio de contraseña.</b> Un JWT firmado sigue válido porque cambiar el hash no cambia su firma ni su fecha exp. AuthServiceImpl.cambiarPassword no mantiene sesiones ni revoca tokens, y JwtAuthenticationFilter comprueba firma, vigencia y cuenta habilitada. Una mejora sería añadir tokenVersion al usuario y al JWT, incrementarla al cambiar la contraseña y comparar versiones en cada petición; los tokens antiguos quedarían invalidados. No se implementó esa política porque B-04 verifica expresamente la continuidad del JWT anterior.'),para('<b>3. Secreto JWT expuesto.</b> Quien conoce la clave de firma puede fabricar tokens válidos e intentar suplantar usuarios existentes; la recarga de permisos desde la base no evita la suplantación de un administrador. La respuesta inmediata consiste en rotar JWT_SECRET por una clave nueva, reiniciar o desplegar todas las instancias y exigir nuevos logins: T-03 demuestra el rechazo de tokens firmados con la clave anterior. Después corresponde retirar el secreto del repositorio y su historial, revisar accesos y mantener la clave fuera del código. JwtService y la configuración por entorno concentran esta gestión.'),para('<b>4. Autorización por método.</b> @PreAuthorize en ReporteController expresa REPORTES_VER junto a las operaciones que protege y evita depender únicamente de la coincidencia de una URL en SecurityConfig. Facilita reconocer la regla al leer el controlador y probarla por operación. Como desventaja, distribuye las reglas entre clases, exige activar la seguridad de métodos y requiere cuidar la consistencia al agregar controladores. Las pruebas P-03 y P-04 contrastan el rechazo de vendedor/almacén con el acceso del administrador.'),evidence(91,400,'Figura 3. C-01: preflight OPTIONS 200 desde localhost:4200 con Authorization autorizado.'),para('Referencias de implementación: AuthController, AuthServiceImpl.cambiarPassword, UsuarioServiceImpl.asignarRoles/cambiarEstado, JwtAuthenticationFilter.doFilterInternal, JwtService, SecurityConfig y ReporteController. Pruebas: SeguridadIntegrationTest, JwtServiceTest y PermisosPorRolTest. Matriz de 28 casos y evidencia por aspecto en el documento complementario.',SMALL)])
build(OUT/'informe-s10.pdf',story)
assert len(PdfReader(OUT/'informe-s10.pdf').pages)==3,'Main report must be 3 pages'
shutil.copy2(OUT/'informe-s10.pdf',OUT/'DelCarpio_LP2_S10_Autonoma.pdf')

# Companion document starts with the eight-column matrix, then all original screenshots grouped by aspect.
story=[para('Matriz de seguridad y evidencias',H1),para('PharmaBackend · Sesión 10 · 18 de octubre de 2026',H2),para('28 casos: 16 base, cuatro propios y ocho de contraseña, roles y estado. Un caso puede agrupar varias peticiones. Pasa significa coincidencia entre resultado esperado y obtenido, incluidos los rechazos previstos. Las referencias E001-E092 identifican figuras; las verificaciones automatizadas complementarias se distinguen en la matriz.',SMALL),table([head]+rows,[42,63,81,101,143,186,40,109],6.7),PageBreak(),para('Evidencias',H1),Spacer(1,4)]
groups=[('Autenticación y perfil',lambda c:c.startswith('A-') or c=='AUX'),('Integridad, firma y expiración del JWT',lambda c:c.startswith('T-')),('Restauración de condiciones de prueba',lambda c:c=='REST'),('Permisos por rol',lambda c:c.startswith('P-')),('Creación y validación de usuarios',lambda c:c.startswith('U-')),('Cambio de contraseña',lambda c:c in ['B-01','B-02','B-03','B-04']),('Asignación y reemplazo de roles',lambda c:c in ['B-05','B-06']),('Salvaguardas administrativas',lambda c:c=='B-07'),('Activación y desactivación de cuentas',lambda c:c=='B-08'),('CORS y acceso desde la SPA',lambda c:c in ['PREP','C-01'])]
category_rows=[[para('• '+groups[i][0]),para('• '+groups[i+5][0])] for i in range(5)]
category_list=Table(category_rows,colWidths=[380,380],hAlign='LEFT')
category_list.setStyle(TableStyle([('VALIGN',(0,0),(-1,-1),'TOP'),('LEFTPADDING',(0,0),(-1,-1),0),('TOPPADDING',(0,0),(-1,-1),0),('BOTTOMPADDING',(0,0),(-1,-1),0)]))
story.extend([category_list,Spacer(1,8)])
for group_index,(group,predicate) in enumerate(groups):
 if group_index: story.append(PageBreak())
 story.append(para(group,H1))
 entries=[m for m in manifest if predicate(m['caso'])]
 for i,m in enumerate(entries):
  if i:story.extend([PageBreak(),para(group,H2)])
  story.extend([para(f"E{m['numero']:03} · {m['caso']} · {escape(m['titulo'])}",H2),para(escape(m['descripcion'])),evidence(m['numero'],590 if group_index==0 and i==0 else 740)])
build(OUT/'DelCarpio_LP2_S10_Matriz_Evidencias.pdf',story,landscape(A4))
print(json.dumps({'mainPages':len(PdfReader(OUT/'informe-s10.pdf').pages),'evidencePages':len(PdfReader(OUT/'DelCarpio_LP2_S10_Matriz_Evidencias.pdf').pages),'cases':len(rows),'figures':len(manifest)}))

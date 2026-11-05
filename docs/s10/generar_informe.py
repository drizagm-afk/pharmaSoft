"""Generate the S10 practical report, preserving the supplied evidence pixels."""
from pathlib import Path
from io import BytesIO
from html import escape
from datetime import datetime,timezone,timedelta
import json,hashlib,shutil
from PIL import Image as PILImage
from pypdf import PdfReader,PdfWriter
from reportlab.pdfgen import canvas
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import SimpleDocTemplate,Paragraph,Spacer,Table,TableStyle,Image,PageBreak,KeepTogether
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.pagesizes import A4,landscape
from reportlab.lib import colors

ROOT=Path(__file__).resolve().parents[2]
FRONT=ROOT.parent/'(IV - Class) PharmaFrontend'
SOURCE=Path(r'C:/Users/DRIZA/Documents/UNI/Evidence')
DEST=ROOT/'docs/s10/evidencias';DEST.mkdir(parents=True,exist_ok=True)
NAVY=colors.HexColor('#12325b');GOLD=colors.HexColor('#eda900')
NAME='Kevin Eduardo Del Carpio Alegría'
pdfmetrics.registerFont(TTFont('S10Arial',r'C:/Windows/Fonts/arial.ttf'))
pdfmetrics.registerFont(TTFont('S10ArialBold',r'C:/Windows/Fonts/arialbd.ttf'))
pdfmetrics.registerFontFamily('S10Arial',normal='S10Arial',bold='S10ArialBold',italic='S10Arial',boldItalic='S10ArialBold')
def style(size=9.5,bold=False,color=colors.black):
 return ParagraphStyle('s10',fontName='S10ArialBold' if bold else 'S10Arial',fontSize=size,leading=size+3,textColor=color,spaceAfter=7)
BODY=style();SMALL=style(8);H=style(13,True,NAVY)

# group, case, caption and brief description, in creation-time order.
DATA=[
('Autenticación y acceso público','02','Catálogo sin token','GET productos devuelve 401 y el mensaje Debe iniciar sesión para acceder a este recurso.'),
('Autenticación y acceso público','03','Contraseña incorrecta','Login de admin con contraseña incorrecta: 401 y mensaje genérico de credenciales.'),
('Autenticación y acceso público','04','Cuenta inactiva','Login de inactivo: 401 con el mismo mensaje de credenciales incorrectas.'),
('Autenticación y acceso público','01','Health público','GET /api/health responde 200 y confirma el funcionamiento del backend sin autenticación.'),
('Autenticación y acceso público','05','Username obligatorio','Username vacío: 400 con validationErrors.username y el mensaje El usuario es obligatorio.'),
('Autenticación y acceso público','06','Login vendedor','Respuesta 200 con JWT Bearer, duración 3600 segundos, username vendedor y rol VENDEDOR.'),
('Autenticación y acceso público','06','Login vendedor: comprobación adicional','Nueva respuesta 200 de login vendedor con token y sus datos de autenticación.'),
('Autenticación y acceso público','06','Permisos del vendedor','Vista completa de los cinco permisos del vendedor: catálogo de lectura, clientes y ventas.'),
('JWT y perfil autenticado','08','Perfil del usuario autenticado','GET auth/me con tokenVendedor devuelve 200 y datos del empleado, roles y permisos, sin contraseña.'),
('JWT y perfil autenticado','07','Claims y duración del JWT','Decodificación local: sub vendedor, authorities, iat, exp y diferencia de 3600 segundos.'),
('JWT y perfil autenticado','08','Perfil autenticado: comprobación adicional','La respuesta de auth/me presenta el perfil plano y los permisos efectivos del vendedor.'),
('Permisos por rol','09','Lectura de catálogo permitida','Vendedor obtiene 200 y contenido paginado de productos.'),
('Permisos por rol','10','Borrado de catálogo denegado','DELETE productos/1 con token vendedor responde 403: No tiene permisos para realizar esta operación.'),
('Permisos por rol','11','Venta registrada por vendedor','POST ventas crea la venta 54: Vitamina D x1, precio y total S/12.50, estado REGISTRADA, HTTP 201.'),
('Permisos por rol','12','Reporte denegado al vendedor','El token del vendedor recibe 403 al consultar el reporte de ventas por categoría.'),
('Permisos por rol','AUX','Autenticación de almacén','Login de almacen responde 200 con rol ALMACENERO y permisos CATALOGO_ESCRIBIR y CATALOGO_LEER.'),
('Permisos por rol','13','Solicitud de venta desde almacén','Preparación del Body: cliente 11 y producto 133, cantidad 1, antes de enviar la solicitud.'),
('Permisos por rol','13','Venta denegada a almacén','El POST de ventas enviado como almacen recibe 403, sin registrar la operación.'),
('Permisos por rol','14','Preparación de lectura del producto','GET productos/134 configurado con tokenAlmacen antes de obtener los valores actuales.'),
('Permisos por rol','14','Lectura actual del producto','Protector solar: GET 200, precio S/18.90, stock 19, categoría Higiene y estado activo.'),
('Permisos por rol','14','Actualización permitida a almacén','PUT productos/134 responde 200 y conserva los valores comerciales del producto.'),
('Permisos por rol','AUX','Autenticación de administrador','Login admin: 200, rol ADMIN y los ocho permisos de la aplicación.'),
('Permisos por rol','15','Reporte permitido al administrador','GET del reporte con tokenAdmin responde 200: Higiene 10 unidades por S/130.80 y Vitaminas 10 por S/125.00.'),
('Roles múltiples','16','Login de supervisor','Respuesta 200 con roles ALMACENERO y VENDEDOR, y la unión de sus permisos.'),
('Roles múltiples','17','Supervisor registra una venta','POST ventas crea la venta 55 por S/12.50: cliente 11, producto 133 y cantidad 1; HTTP 201.'),
('Roles múltiples','17','Supervisor consulta el producto','GET productos/134 con tokenSupervisor: 200 y valores actuales del producto.'),
('Roles múltiples','17','Supervisor actualiza catálogo','PUT productos/134 con tokenSupervisor: 200, además del permiso de venta demostrado en el mismo caso.'),
('Roles múltiples','18','Reporte denegado al supervisor','TokenSupervisor recibe 403: la combinación de sus dos roles no concede REPORTES_VER.'),
('Usuarios y relaciones','19','Empleados y disponibilidad de cuenta','GET empleados con tokenAdmin responde 200; María Huamán, empleado 6, aparece sin usuario.'),
('Usuarios y relaciones','19','Roles y permisos administrativos','GET roles devuelve 200 e incluye ADMIN y ALMACENERO con sus permisos.'),
('Usuarios y relaciones','19','Rol VENDEDOR disponible','Continuación de la respuesta de roles: VENDEDOR, id 2, con sus permisos de clientes y ventas.'),
('Usuarios y relaciones','22','Rechazo de rol inexistente','Alta con rolIds 2 y 99 recibe 404: Roles no encontrados: [99]. Se ejecuta antes del alta válida.'),
('Usuarios y relaciones','20','Creación de la cuenta mhuaman','POST usuarios devuelve 201: usuario 21, empleado 6, rol VENDEDOR y permisos, sin contraseña en la respuesta.'),
('Usuarios y relaciones','20','Hash BCrypt persistido','Consulta Oracle de mhuaman: prefijo $2a$10$, longitud de hash 60 y empleado_id 6.'),
('Usuarios y relaciones','20','Relación de usuario y rol','Consulta Oracle une mhuaman, empleado 6 y rol 2 VENDEDOR mediante usuario_roles.'),
('Usuarios y relaciones','21','Un empleado, una cuenta','Intento de mhuaman2 para empleado 6 recibe 409: El empleado Maria Huaman Torres ya tiene un usuario.'),
('Integridad y expiración del token','23','Firma de token alterada','GET productos con tokenAlterado responde 401: El token no es válido.'),
('Integridad y expiración del token','24','Primera emisión de token corto','Login vendedor con duración configurada de 60 segundos y respuesta 200.'),
('Integridad y expiración del token','24','Acceso con token corto','GET productos con tokenCorto obtiene 200 en la comprobación inicial.'),
('Integridad y expiración del token','24','Nueva emisión de token de un minuto','Segunda secuencia de emisión: login vendedor 200 y expiraEnSegundos 60.'),
('Integridad y expiración del token','24','Acceso válido de la segunda secuencia','El GET con tokenCorto obtiene 200 después de la nueva emisión.'),
('Integridad y expiración del token','24','Rechazo tras la expiración','El mismo tipo de petición con tokenCorto devuelve 401: El token ha expirado. Inicie sesión nuevamente.'),
('Integridad y expiración del token','24','Expiración: comprobación adicional','Nueva comprobación del token corto vencido: 401 con el mismo mensaje de expiración.'),
('Persistencia y pruebas unitarias','FLYWAY','Historial de migraciones','Oracle muestra baseline de versión 1 y V2 seguridad aplicada como SQL, ambas con success 1.'),
('Persistencia y pruebas unitarias','TEST','Pruebas unitarias de seguridad','JwtServiceTest: cinco pruebas; PermisosPorRolTest: cuatro. Nueve ejecutadas, sin fallos, errores ni omisiones; BUILD SUCCESS.'),
]
files=sorted(SOURCE.glob('*.png'),key=lambda f:(f.stat().st_birthtime,f.name));assert len(files)==len(DATA)==45
manifest=[]
tz=timezone(timedelta(hours=-5))
for i,(f,(group,case,title,desc)) in enumerate(zip(files,DATA),1):
 target=DEST/f'evidencia-{i:02}.png';shutil.copy2(f,target);stat=f.stat()
 manifest.append({'original':str(f),'archivo':target.name,'grupo':group,'caso':case,'titulo':title,'descripcion':desc,
  'creado':datetime.fromtimestamp(stat.st_birthtime,tz).isoformat(),'modificado':datetime.fromtimestamp(stat.st_mtime,tz).isoformat(),
  'sha256':hashlib.sha256(f.read_bytes()).hexdigest(),'orden':i})
(DEST/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

story=[]
def p(text,s=BODY):story.append(Paragraph(text,s))
def h(text):p(text,H)
def figure(n,caption,width=440):
 path=DEST/f'evidencia-{n:02}.png'
 with PILImage.open(path) as im:iw,ih=im.size
 img=Image(str(path),width=width,height=width*ih/iw);img.hAlign='CENTER'
 story.append(KeepTogether([img,Paragraph(caption,SMALL)]))
def table(rows,widths,size=8):
 cells=[[Paragraph(escape(str(v)),style(size,i==0,colors.white if i==0 else colors.black)) for v in row] for i,row in enumerate(rows)]
 t=Table(cells,colWidths=widths,repeatRows=1,hAlign='LEFT')
 t.setStyle(TableStyle([('BACKGROUND',(0,0),(-1,0),NAVY),('ROWBACKGROUNDS',(0,1),(-1,-1),[colors.white,colors.HexColor('#f1f4f8')]),('GRID',(0,0),(-1,-1),.3,colors.HexColor('#bacce5')),('VALIGN',(0,0),(-1,-1),'TOP'),('TOPPADDING',(0,0),(-1,-1),3),('BOTTOMPADDING',(0,0),(-1,-1),3)]));story.extend([t,Spacer(1,8)])
def header(c,d):
 c.setFillColor(NAVY);c.setFont('S10ArialBold',8);c.drawString(42,812,'LP II · Guía práctica · Sesión 10');c.setStrokeColor(GOLD);c.line(42,804,A4[0]-42,804)

story.extend([Image(str(FRONT/'docs/s8/assets/upeu-logo.png'),width=110,height=39.6),Spacer(1,10)])
p('Actividad práctica - Sesión 10',style(20,True,NAVY))
p('Seguridad backend: autenticación JWT y permisos',style(12,True,NAVY))
p(NAME+' · Ejecución: 15 de octubre de 2026',SMALL)
h('1. Objetivo y flujo de autenticación')
p('Proteger PharmaBackend mediante cuentas de empleados, contraseñas BCrypt, tokens JWT y permisos agrupados en roles. La verificación comprende las 24 pruebas de seguridad de la guía: acceso público, credenciales, perfil, autorización, roles múltiples, alta de usuarios, integridad y expiración del token.')
p('POST /api/v1/auth/login autentica las credenciales y devuelve un JWT Bearer, duración, nombre, roles y permisos. Cada petición protegida envía el token en Authorization. La cadena sin estado valida firma y expiración y carga la cuenta actual desde Oracle antes de autorizar el acceso.')
figure(8,'Figura 1. Login de vendedor: token Bearer, duración de 3600 segundos, rol y cinco permisos.')
p('Un 401 indica ausencia de autenticación válida; un 403 identifica una cuenta autenticada sin el permiso requerido. Contraseña incorrecta y cuenta inactiva devuelven el mismo mensaje para evitar revelar la causa concreta del rechazo.',SMALL)
story.append(PageBreak());h('2. Modelo de seguridad y autorización')
p('Empleado y Usuario mantienen una relación 1 a 1 mediante empleado_id único. Usuario y Rol se relacionan mediante usuario_roles. Las contraseñas se almacenan como hash BCrypt; los DTO de respuesta exponen datos planos y nunca incluyen contraseña ni hash. PermisosPorRol calcula la unión de permisos y añade ROLE_ para cada rol.')
table([['Operación','ADMIN','VENDEDOR','ALMACENERO','Ambos roles'],['Leer catálogo','Sí','Sí','Sí','Sí'],['Editar catálogo','Sí','No','Sí','Sí'],['Clientes y ventas','Sí','Sí','No','Sí'],['Reportes','Sí','No','No','No'],['Usuarios y roles','Sí','No','No','No']],[170,69,87,101,84])
p('SecurityConfig aplica las reglas por URL y método. ReporteController exige REPORTES_VER con @PreAuthorize. El supervisor combina VENDEDOR y ALMACENERO: puede vender y actualizar productos, pero esa unión no incluye acceso a reportes.')
figure(35,'Figura 2. Persistencia de mhuaman: empleado 6 y rol VENDEDOR mediante la tabla de unión.')
p('La cuenta mhuaman se crea para María Huamán Torres con id 21 y empleado 6. Oracle confirma prefijo BCrypt $2a$10$, hash de 60 caracteres y rol 2. Un segundo username para el mismo empleado se rechaza con 409.',SMALL)
story.append(PageBreak());h('3. Resultados de las 24 pruebas')
p('Los códigos esperados de rechazo son resultados correctos de seguridad. La tabla reúne cada caso y el resultado observado en las peticiones y consultas.',SMALL)
rows=[
('01','Health público','200 sin token.'),('02','Catálogo sin token','401: debe iniciar sesión.'),
('03','Password incorrecto','401: usuario o contraseña incorrectos.'),('04','Cuenta inactiva','401 con el mismo mensaje genérico.'),
('05','Username vacío','400 con validationErrors.username.'),('06','Login vendedor','200, Bearer, 3600 segundos y rol VENDEDOR.'),
('07','Claims locales','sub, authorities, iat y exp; exp - iat = 3600.'),('08','Perfil autenticado','200; empleado, roles y permisos sin contraseña.'),
('09','Vendedor lee productos','200, respuesta paginada.'),('10','Vendedor no borra','403 al DELETE de producto.'),
('11','Vendedor registra venta','201: venta 54, Vitamina D x1, S/12.50.'),('12','Vendedor sin reporte','403 por permiso REPORTES_VER.'),
('13','Almacén no vende','403 al POST de venta.'),('14','Almacén actualiza catálogo','200 al PUT de producto 134.'),
('15','Administrador ve reporte','200, categorías e importes.'),('16','Supervisor con dos roles','200 y unión de permisos sin REPORTES_VER.'),
('17','Supervisor vende y edita','201 venta 55 por S/12.50; PUT 200.'),('18','Supervisor sin reporte','403; ninguno de sus roles concede el permiso.'),
('19','Empleados y roles','200 en ambas consultas; María comienza sin cuenta.'),('20','Alta y BCrypt','201 mhuaman; hash de 60 caracteres y relación al rol.'),
('21','Empleado con cuenta','409 al intentar mhuaman2 para empleado 6.'),('22','Rol inexistente','404: Roles no encontrados: [99].'),
('23','Firma alterada','401: El token no es válido.'),('24','Token expirado','Token de 60 segundos: acceso 200 y posteriormente 401.'),
]
table([['Caso','Verificación','Resultado observado']]+[('S10-'+n,a,b) for n,a,b in rows],[48,160,303],7.8)
story.append(PageBreak());h('4. Resultados y consistencia')
p('Vendedor registró la venta 54 y supervisor la venta 55. Ambas corresponden al cliente 11, producto 133 y cantidad 1: 12.50 x 1 = S/12.50 por venta. Las actualizaciones autorizadas del producto 134 conservaron precio S/18.90 y stock 19. Almacén recibió 403 al intentar vender; vendedor recibió 403 al intentar borrar catálogo.')
p('La cuenta de supervisor demuestra la suma de permisos de sus dos roles sin elevarlos a ADMIN. El administrador accede al reporte, donde la captura muestra Higiene: 10 unidades por S/130.80 y Vitaminas: 10 por S/125.00. Estos valores corresponden al momento de esa consulta.')
h('5. Integridad y expiración del JWT')
p('El JWT ordinario presenta duración de 3600 segundos. La firma alterada devuelve 401. Para la expiración se emitió un token de 60 segundos: en la segunda secuencia de comprobación el acceso inicial devolvió 200 y el acceso posterior devolvió 401 con el mensaje El token ha expirado. Inicie sesión nuevamente. Las comprobaciones repetidas se mantienen dentro del mismo caso.')
figure(42,'Figura 3. El acceso con token corto vencido responde 401 con un mensaje específico de expiración.')
h('6. Persistencia, pruebas y conclusiones')
p('El historial Flyway muestra baseline de versión 1 y migración V2 seguridad aplicada con éxito. Las pruebas unitarias de JwtService y PermisosPorRol ejecutan nueve casos sin fallos, errores ni omisiones. La API distingue autenticación y autorización, aplica permisos a cada operación y mantiene las relaciones de cuenta, empleado y roles. Las evidencias siguientes se organizan por aspecto, conservando las secuencias de validación.',SMALL)

b=BytesIO();SimpleDocTemplate(b,pagesize=A4,leftMargin=42,rightMargin=42,topMargin=52,bottomMargin=40).build(story,onFirstPage=header,onLaterPages=header);b.seek(0)
writer=PdfWriter();writer.append(PdfReader(b),import_outline=False);main_pages=len(writer.pages)
writer.add_outline_item('Informe de implementación y verificación',0)
groups=['Autenticación y acceso público','JWT y perfil autenticado','Permisos por rol','Roles múltiples','Usuarios y relaciones','Integridad y expiración del token','Persistencia y pruebas unitarias']
ordered=sorted(manifest,key=lambda v:(groups.index(v['grupo']),v['creado']))
evidence_parent=writer.add_outline_item('Evidencias',len(writer.pages));current=None;pages={}
for index,item in enumerate(ordered):
 w,h=landscape(A4);buf=BytesIO();c=canvas.Canvas(buf,pagesize=(w,h));y=h-30;c.setFillColor(NAVY)
 if index==0:c.setFont('S10ArialBold',24);c.drawString(28,y,'Evidencias');y-=34
 if item['grupo']!=current:
  current=item['grupo'];parent=writer.add_outline_item(current,len(writer.pages),parent=evidence_parent)
  c.setFont('S10ArialBold',18);c.drawString(28,y,current);y-=29
 title=('S10-'+item['caso']+' · ' if item['caso'].isdigit() else '')+item['titulo']
 c.setFont('S10ArialBold',11);c.drawString(28,y,title);y-=10;c.setStrokeColor(GOLD);c.setLineWidth(1.3);c.line(28,y,w-28,y);y-=12
 desc=Paragraph(escape(item['descripcion']),style(9));_,ph=desc.wrap(w-56,80);desc.drawOn(c,28,y-ph);top=y-ph-12
 path=DEST/item['archivo']
 with PILImage.open(path) as im:iw,ih=im.size
 factor=min((w-80)/iw,(top-52)/ih);dw,dh=iw*factor,ih*factor
 c.drawImage(str(path),(w-dw)/2,40+(top-40-dh)/2,width=dw,height=dh,preserveAspectRatio=True)
 c.save();buf.seek(0);pages[item['archivo']]=len(writer.pages)+1
 writer.add_outline_item(title,len(writer.pages),parent=parent);writer.append(PdfReader(buf),import_outline=False)
total=len(writer.pages)
for i,page in enumerate(writer.pages,1):
 w=float(page.mediabox.width);buf=BytesIO();c=canvas.Canvas(buf,pagesize=(w,float(page.mediabox.height)));c.setFont('S10Arial',7);c.setFillColor(colors.black);c.drawString(28,16,NAME);c.drawRightString(w-28,16,f'Página {i} de {total}');c.save();buf.seek(0);page.merge_page(PdfReader(buf).pages[0])
output=ROOT/'docs/DelCarpio_LP2_S10_Practica.pdf'
writer.add_metadata({'/Title':'S10 · Seguridad backend: implementación y pruebas','/Author':NAME});writer.write(output)
(ROOT/'docs/s10/paginacion-informe.json').write_text(json.dumps({'paginas':total,'informe_paginas':main_pages,'evidencias':pages},ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(f'Created {total} pages: {main_pages} report pages and {len(manifest)} evidence pages.')

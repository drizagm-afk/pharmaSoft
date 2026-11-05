"""Prepare pending manual cases and a token-free Postman collection; never call the API."""
from pathlib import Path
import json,re
ROOT=Path(__file__).resolve().parents[2]
CASES=[
('A-01','Autenticación','admin, vendedor, almacen, supervisor','POST /auth/login (cuatro cuentas)','200 con roles y permisos; supervisor recibe la unión'),
('A-02','Autenticación','inexistente y admin con clave incorrecta','POST /auth/login (dos intentos)','401: Usuario o contraseña incorrectos en ambos'),
('A-03','Autenticación','inactivo','POST /auth/login','401 con mensaje genérico de credenciales'),
('A-04','Autenticación','VENDEDOR','POST /auth/login','200; regla sin distinguir mayúsculas'),
('A-05','Autenticación (propio)',' vendedor con espacios','POST /auth/login','200; trim del username'),
('T-01','Token','token vendedor sin Bearer','GET /productos','401: Debe iniciar sesión para acceder a este recurso'),
('T-02','Token','payload cambiado, firma original','GET /productos','401: El token no es válido'),
('T-03','Token','token previo al cambio de secreto','GET /productos','401: El token no es válido'),
('T-04','Token','token de un minuto vencido','GET /productos','401: El token ha expirado. Inicie sesión nuevamente'),
('T-05','Token','token vendedor válido','Decodificar payload localmente','sub, authorities, iat y exp correctos; sin datos sensibles'),
('T-06','Token (propio)','Bearer abc.def.ghi','GET /productos','401: El token no es válido (formato malformado)'),
('P-01','Permisos','vendedor','POST, PUT y DELETE /categorias','403 en las tres operaciones'),
('P-02','Permisos','almacen','GET /clientes; POST /ventas','403 en ambas'),
('P-03','Permisos','vendedor y almacen','GET /reportes/productos-mas-vendidos','403 por autorización de método para ambos'),
('P-04','Permisos','admin','GET de cada módulo','2xx en categorías, productos, clientes, ventas, reportes y usuarios/empleados/roles'),
('P-05','Permisos (propio)','vendedor','POST /usuarios','403 sin crear cuenta'),
('U-01','Usuarios','admin / empleado de prueba sin cuenta','POST /usuarios: password abcdefgh; rolIds []','400 en ambos; validación password y rolIds'),
('U-02','Usuarios','admin / empleado ocupado y empleado de prueba libre','POST /usuarios; comprobar Oracle','409 empleado ocupado; luego 201, hash BCrypt y relación usuario_roles'),
('U-03','Usuarios (propio)','admin / empleado de prueba libre','POST /usuarios con password de más de 72 bytes UTF-8','409: La contraseña no puede superar 72 bytes'),
('C-01','CORS','token vendedor / origen localhost:4200','OPTIONS previo a GET /productos','OPTIONS 200 y GET 200 o 401; sin error de CORS'),
('B-01','Contraseña','cuenta de prueba','PUT /auth/password: actual incorrecta','409: La contraseña actual no es correcta; hash intacto'),
('B-02','Contraseña','cuenta de prueba','PUT /auth/password: nueva igual','409: La nueva contraseña debe ser distinta de la actual'),
('B-03','Contraseña','cuenta de prueba','PUT /auth/password: nueva inválida','400 por formato; 409 si supera 72 bytes UTF-8'),
('B-04','Contraseña','cuenta de prueba y token previo','PUT /auth/password y logins viejo/nuevo; GET con token anterior','204; BCrypt; login viejo 401, nuevo 200; token anterior 200'),
('B-05','Roles','admin y mismo token de cuenta de prueba','PUT /usuarios/{id}/roles; PUT producto; consultar unión','200, conjunto reemplazado y permisos actuales sin nuevo login'),
('B-06','Roles','admin','Roles vacíos/nulos; usuario o rol inexistente','400 validaciones; 404 inexistentes'),
('B-07','Salvaguardas','admin / cuenta de prueba','Autoquitar ADMIN, autodesactivar; llamadas no admin','409 en autoacciones; 403 a no admin'),
('B-08','Estado','admin y token previo de cuenta de prueba','PATCH estado; acceso/login; reactivar; variantes inválidas','200 estado; 401 deshabilitado/login; reactivar 200; 404 usuario y 400 estado ausente'),
]
head=['ID','Grupo','Usuario / token','Petición','Resultado esperado (código y mensaje)','Resultado obtenido','Estado','Evidencia']
lines=['# Matriz de seguridad S10 - preparación manual','',
 '28 casos: 16 base, cuatro propios de grupos distintos y ocho B. Los resultados manuales están pendientes; las automatizadas no se marcan como capturas ejecutadas.',
 '', '| '+' | '.join(head)+' |','| '+' | '.join(['---']*8)+' |']
for row in CASES:lines.append('| '+' | '.join((*row,'Pendiente de ejecución manual','Pendiente','Pendiente'))+' |')
lines+=['','Orden de ejecución: A; T con restauración de secreto/duración y nuevos logins; P; U-01, U-03, U-02; C; B-01 a B-08.',
 'U-02 utiliza el empleado separado del fixture, conservando la cuenta original de María. T-06 se concreta como JWT malformado para reproducir el caso propio sin herramientas de firma adicionales. Registrar subcapturas por cada petición de los casos compuestos.']
(ROOT/'docs/matriz-seguridad-s10.md').write_text('\n'.join(lines)+'\n',encoding='utf-8')
groups={};items=[]
def req(group,name,method,path,status,body=None,token=None,after='',before='',header=None):
 headers=[] if body is None else [{'key':'Content-Type','value':'application/json'}]
 if header:headers.append({'key':'Authorization','value':header})
 r={'method':method,'url':'{{baseUrl}}'+path,'header':headers,'auth':{'type':'noauth'} if not token else
    {'type':'bearer','bearer':[{'key':'token','value':'{{'+token+'}}','type':'string'}]}}
 if body is not None:
  raw=body if isinstance(body,str) else json.dumps(body,ensure_ascii=False,indent=2)
  raw=re.sub(r'"(\{\{(?:empleadoPruebaId|usuarioPruebaId|usuarioAdminId|rol\w+Id)\}\})"',r'\1',raw)
  r['body']={'mode':'raw','raw':raw,'options':{'raw':{'language':'json'}}}
 events=[{'listen':'test','script':{'type':'text/javascript','exec':(f'pm.test("HTTP {status}", () => pm.response.to.have.status({status}));\n'+after).splitlines()}}]
 if before:events.insert(0,{'listen':'prerequest','script':{'type':'text/javascript','exec':before.splitlines()}})
 item={'name':name,'request':r,'event':events};groups.setdefault(group,[]).append(item)
def login(group,name,user,password,var=None,expected=200,seconds=None):
 after='';before=''
 if var:
  before=f'pm.collectionVariables.unset("{var}");'
  condition='pm.response.code === 200'+(f' && pm.response.json().expiraEnSegundos === {seconds}' if seconds else '')
  after=f'if ({condition}) pm.collectionVariables.set("{var}", pm.response.json().token);'
 if seconds:after+=f'\npm.test("Duración {seconds}s", () => pm.expect(pm.response.json().expiraEnSegundos).to.eql({seconds}));'
 req(group,name,'POST','/auth/login',expected,{'username':user,'password':password},after=after,before=before)
for u,p,v in [('admin','Admin2026*','tokenAdmin'),('vendedor','Vendedor2026*','tokenVendedor'),('almacen','Almacen2026*','tokenAlmacen'),('supervisor','Supervisor2026*','tokenSupervisor')]:
 login('A · Autenticación','A-01 · Login '+u,u,p,v)
req('A · Autenticación','Preparación · Perfil admin','GET','/auth/me',200,token='tokenAdmin',after='if(pm.response.code===200) pm.collectionVariables.set("usuarioAdminId", pm.response.json().id);')
login('A · Autenticación','A-02a · Usuario inexistente','no_existe_s10','Incorrecta2026',expected=401)
login('A · Autenticación','A-02b · Password incorrecto','admin','Incorrecta2026',expected=401)
login('A · Autenticación','A-03 · Cuenta inactiva','inactivo','Inactivo2026*',expected=401)
login('A · Autenticación','A-04 · Mayúsculas','VENDEDOR','Vendedor2026*')
login('A · Autenticación','A-05 · Espacios exteriores',' vendedor ','Vendedor2026*')
req('T · Token','T-01 · Sin Bearer','GET','/productos',401,header='{{tokenVendedor}}')
req('T · Token','T-02 · Payload alterado','GET','/productos',401,token='tokenAlterado',before='''const p=pm.collectionVariables.get("tokenVendedor").split(".");
// Cambiar el primer carácter del segmento payload manteniendo header y firma.
p[1]=(p[1][0]==="A"?"B":"A")+p[1].slice(1);
pm.collectionVariables.set("tokenAlterado",p.join("."));''')
req('T · Token','T-03 · DESPUÉS de cambiar secreto · token previo','GET','/productos',401,token='tokenVendedor')
login('T · Token','T-04 · DESPUÉS de configurar 1 minuto · emitir token','vendedor','Vendedor2026*','tokenCorto',seconds=60)
req('T · Token','T-04 · Acceso inicial','GET','/productos',200,token='tokenCorto')
req('T · Token','T-04 · ESPERAR dos minutos · mismo token','GET','/productos',401,token='tokenCorto')
req('T · Token','T-06 · JWT malformado','GET','/productos',401,header='Bearer abc.def.ghi')
for u,p,v in [('admin','Admin2026*','tokenAdmin'),('vendedor','Vendedor2026*','tokenVendedor'),('almacen','Almacen2026*','tokenAlmacen')]:
 login('Restauración · SECRETO Y DURACIÓN normales','Renovar '+u,u,p,v,seconds=3600)
for method in ['POST','PUT','DELETE']:
 req('P · Permisos','P-01 · '+method+' categorías',''+method,'/categorias'+('' if method=='POST' else '/9'),403,{'nombre':'Prueba','estado':True} if method!='DELETE' else None,'tokenVendedor')
req('P · Permisos','P-02a · Almacén no lee clientes','GET','/clientes',403,token='tokenAlmacen')
req('P · Permisos','P-02b · Almacén no vende','POST','/ventas',403,{'clienteId':11,'detalles':[{'productoId':133,'cantidad':1}]},'tokenAlmacen')
for token in ['tokenVendedor','tokenAlmacen']:req('P · Permisos','P-03 · Reporte · '+token,'GET','/reportes/productos-mas-vendidos',403,token=token)
for path in ['/categorias','/productos','/clientes','/ventas/buscar','/reportes/ventas-por-categoria','/usuarios','/empleados','/roles']:
 after=''
 if path=='/empleados':after='if(pm.response.code===200){const e=pm.response.json().find(e=>e.dni==="56778899"); if(e)pm.collectionVariables.set("empleadoPruebaId",e.id);}'
 if path=='/roles':after='if(pm.response.code===200)for(const r of pm.response.json()){const k={ADMIN:"rolAdminId",VENDEDOR:"rolVendedorId",ALMACENERO:"rolAlmacenId"}[r.nombre];if(k)pm.collectionVariables.set(k,r.id);}'
 req('P · Permisos','P-04 · Admin '+path,'GET',path,200,token='tokenAdmin',after=after)
valid={'username':'autonoma_s10','password':'Prueba2026*','empleadoId':'{{empleadoPruebaId}}','rolIds':['{{rolVendedorId}}']}
req('P · Permisos','P-05 · Vendedor no crea usuarios','POST','/usuarios',403,valid,'tokenVendedor')
req('U · Usuarios · 01 y 03 ANTES de 02','U-01a · Password sin números','POST','/usuarios',400,{**valid,'password':'abcdefgh'},'tokenAdmin')
req('U · Usuarios · 01 y 03 ANTES de 02','U-01b · Roles vacíos','POST','/usuarios',400,{**valid,'rolIds':[]},'tokenAdmin')
req('U · Usuarios · 01 y 03 ANTES de 02','U-03 · Más de 72 bytes UTF-8','POST','/usuarios',409,{**valid,'password':'a1'+'ñ'*36},'tokenAdmin')
req('U · Usuarios · 01 y 03 ANTES de 02','U-02a · Empleado que ya tiene cuenta','POST','/usuarios',409,{**valid,'username':'otra_cuenta_s10','empleadoId':6},'tokenAdmin')
req('U · Usuarios · 01 y 03 ANTES de 02','U-02b · CREAR cuenta de prueba','POST','/usuarios',201,valid,'tokenAdmin',after='if(pm.response.code===201)pm.collectionVariables.set("usuarioPruebaId",pm.response.json().id);')
login('U · Usuarios · 01 y 03 ANTES de 02','Preparación B · Login cuenta de prueba','autonoma_s10','Prueba2026*','tokenPrueba')
def password_case(name,actual,new,status):req('B · Contraseña',name,'PUT','/auth/password',status,{'passwordActual':actual,'passwordNueva':new},'tokenPrueba')
password_case('B-01 · Actual incorrecta','Incorrecta2026','NuevaPrueba2026',409)
password_case('B-02 · Nueva igual','Prueba2026*','Prueba2026*',409)
for suffix,new in [('vacía',''),('corta','abc123'),('sin número','abcdefgh'),('sin letra','12345678')]:password_case('B-03 · Nueva '+suffix,'Prueba2026*',new,400)
password_case('B-03 · Exceso UTF-8','Prueba2026*','a1'+'ñ'*36,409)
password_case('B-04a · CAMBIAR password','Prueba2026*','NuevaPrueba2026',204)
login('B · Contraseña','B-04b · Login password anterior','autonoma_s10','Prueba2026*',expected=401)
login('B · Contraseña','B-04c · Login password nueva','autonoma_s10','NuevaPrueba2026','tokenPruebaNuevo')
req('B · Contraseña','B-04d · Token ANTERIOR todavía válido','GET','/auth/me',200,token='tokenPrueba')
password_case('Restaurar password inicial','NuevaPrueba2026','Prueba2026*',204)
login('B · Roles','Renovar cuenta antes de B-05 · no renovar DURANTE B-05','autonoma_s10','Prueba2026*','tokenPrueba')
def current_product(group,name,token):req(group,name,'GET','/productos/134',200,token=token,before='pm.collectionVariables.unset("productoActual");',after='if(pm.response.code===200){const p=pm.response.json();pm.collectionVariables.set("productoActual",JSON.stringify(Object.fromEntries(["nombre","descripcion","precio","stock","categoriaId","estado"].map(k=>[k,p[k]]))));}')
current_product('B · Roles','B-05 · Leer producto actual','tokenPrueba')
req('B · Roles','B-05 · ANTES no puede editar','PUT','/productos/134',403,'{{productoActual}}','tokenPrueba')
def set_roles(group,name,ids,status=200,id='{{usuarioPruebaId}}',token='tokenAdmin'):
 req(group,name,'PUT','/usuarios/'+id+'/roles',status,{'rolIds':ids},token)
set_roles('B · Roles','B-05 · Asignar VENDEDOR y ALMACENERO',['{{rolVendedorId}}','{{rolAlmacenId}}'])
req('B · Roles','B-05 · MISMO token ahora edita','PUT','/productos/134',200,'{{productoActual}}','tokenPrueba')
set_roles('B · Roles','B-05 · Reemplazar por solo ALMACENERO',['{{rolAlmacenId}}'])
req('B · Roles','B-05 · MISMO token ya no lee clientes','GET','/clientes',403,token='tokenPrueba')
set_roles('B · Roles','Restaurar VENDEDOR',['{{rolVendedorId}}'])
for label,ids,status,id in [('vacíos',[],400,'{{usuarioPruebaId}}'),('nulo',[None],400,'{{usuarioPruebaId}}'),('rol inexistente',[999999],404,'{{usuarioPruebaId}}'),('usuario inexistente',['{{rolVendedorId}}'],404,'999999')]:
 set_roles('B · Roles','B-06 · '+label,ids,status,id)
set_roles('B · Salvaguardas','B-07 · Admin intenta quitarse ADMIN',['{{rolVendedorId}}'],409,'{{usuarioAdminId}}')
def state(group,name,value,status=200,id='{{usuarioPruebaId}}',token='tokenAdmin'):
 req(group,name,'PATCH','/usuarios/'+id+'/estado',status,{'estado':value},token)
state('B · Salvaguardas','B-07 · Admin intenta autodesactivarse',False,409,'{{usuarioAdminId}}')
set_roles('B · Salvaguardas','B-07 · No admin intenta roles',['{{rolAlmacenId}}'],403,token='tokenPrueba')
state('B · Salvaguardas','B-07 · No admin intenta estado',False,403,token='tokenPrueba')
login('B · Estado','B-08 · Token previo a desactivar','autonoma_s10','Prueba2026*','tokenPrueba')
state('B · Estado','B-08 · DESACTIVAR cuenta de prueba',False)
req('B · Estado','B-08 · Token previo bloqueado','GET','/auth/me',401,token='tokenPrueba')
login('B · Estado','B-08 · Login deshabilitado','autonoma_s10','Prueba2026*',expected=401)
state('B · Estado','B-08 · REACTIVAR cuenta de prueba',True)
req('B · Estado','B-08 · Token previo tras reactivar','GET','/auth/me',200,token='tokenPrueba')
login('B · Estado','B-08 · Login reactivado','autonoma_s10','Prueba2026*')
state('B · Estado','B-08 · Usuario inexistente',False,404,'999999')
state('B · Estado','B-08 · Estado nulo',None,400)
vars={'baseUrl':'http://localhost:8080/api/v1','rolAdminId':'1','rolVendedorId':'2','rolAlmacenId':'3',
      **{v:'' for v in ['tokenAdmin','tokenVendedor','tokenAlmacen','tokenSupervisor','tokenPrueba','tokenPruebaNuevo','tokenCorto','tokenAlterado','usuarioAdminId','usuarioPruebaId','empleadoPruebaId','productoActual']}}
collection={'info':{'name':'PharmaSoft - S10 Seguridad','schema':'https://schema.getpostman.com/json/collection/v2.1.0/collection.json',
 'description':'Validación manual: ejecutar UNA petición por vez, conservar respuestas y capturar cada caso. Insertar el fixture de empleado antes de P-04. T-03/T-04 requieren reinicios y espera; C-01 se hace desde navegador. U-03 antes de U-02. B-04 cambia y restaura password; B-05 restaura roles; B-08 reactiva. No ejecutar Runner: crearía datos y rompería la secuencia de evidencias.'},
 'variable':[{'key':k,'value':v,'type':'string'} for k,v in vars.items()],
 'item':[{'name':g,'item':its} for g,its in groups.items()]}
out=ROOT/'postman/PharmaSoft-S10-Seguridad.json';out.parent.mkdir(exist_ok=True)
out.write_text(json.dumps(collection,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(f'Prepared {len(CASES)} pending cases and {sum(map(len,groups.values()))} requests; no API execution.')

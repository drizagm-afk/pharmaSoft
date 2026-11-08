# Colección S13 autónoma — Partes C y D

Importar PharmaSoft-S13-Autonoma.postman_collection.json. Es independiente de la
colección de la práctica. Las instrucciones completas están en
../docs/s13-autonoma/etapa-04.md.

1. Completar adminPassword localmente; ejecutar login de carpeta 00.
2. Preparar Redis frío y registrar snapshot ANTES de carpeta 01.
3. Ejecutar SOLO carpeta 02 con Runner: 20 iteraciones, cero delay.
4. Registrar snapshot DESPUÉS en carpeta 03; revisar resumen_*_cache y
   resumen_*_tiempos en variables, ocultando contraseña/token.
5. Ejecutar las otras carpetas individualmente cuando corresponda según el documento.

No correr la colección completa: existen fases incompatibles de Redis encendido y
apagado. La carpeta 05 requiere operaciones previas en la SPA y variables con los
IDs/valores esperados; no hace las escrituras automáticamente. No hay ventas,
eliminaciones ni actualizaciones automáticas en esta colección.

No compartir/exportar una colección utilizada sin vaciar sus contraseñas y tokens.
La versión entregada contiene ambos valores vacíos.

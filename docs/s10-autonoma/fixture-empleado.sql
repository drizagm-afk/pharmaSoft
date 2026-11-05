-- Ejecutar manualmente una vez ANTES de U-01/U-03/U-02.
-- Solo añade un empleado para pruebas; no cambia la cuenta original de María.
-- Si ya existe este DNI, comprobar la consulta final antes de continuar.
INSERT INTO empleados (dni, nombres, apellidos, cargo, email, telefono, estado, fecha_creacion)
SELECT '56778899', 'Maria', 'Huaman Pruebas S10', 'Caja de pruebas',
       's10.pruebas@example.invalid', NULL, true, SYSTIMESTAMP
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM empleados WHERE dni = '56778899');
COMMIT;

SELECT e.id AS empleado_prueba_id, e.dni, e.nombres, e.apellidos,
       u.id AS usuario_prueba_id, u.username
FROM empleados e LEFT JOIN usuarios u ON u.empleado_id = e.id
WHERE e.dni = '56778899';

SELECT id, nombre FROM roles ORDER BY id;

-- Antes del alta de U-02, usuario_prueba_id y username deben ser NULL.
-- Si no lo son, no borrar la cuenta: conservar la evidencia del alta existente
-- o preparar otro empleado con un DNI distinto para una nueva ejecución.

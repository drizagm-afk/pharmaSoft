DECLARE
  v_analgesicos categorias.id%TYPE;
  v_vitaminas categorias.id%TYPE;
  v_higiene categorias.id%TYPE;
  v_inactiva categorias.id%TYPE;

  PROCEDURE producto(p_nombre VARCHAR2, p_descripcion VARCHAR2,
                     p_precio NUMBER, p_stock NUMBER,
                     p_categoria NUMBER, p_estado NUMBER DEFAULT 1) IS
  BEGIN
    INSERT INTO productos
      (nombre, descripcion, precio, stock, categoria_id, estado, fecha_creacion)
    VALUES
      (p_nombre, p_descripcion, p_precio, p_stock, p_categoria, p_estado, SYSDATE);
  END;
BEGIN
  INSERT INTO categorias (nombre, descripcion, estado, fecha_creacion)
  VALUES ('Analgésicos', 'Analgésicos para el dolor', 1, SYSDATE)
  RETURNING id INTO v_analgesicos;

  INSERT INTO categorias (nombre, descripcion, estado, fecha_creacion)
  VALUES ('Vitaminas', 'Vitaminas que fortalecen', 1, SYSDATE)
  RETURNING id INTO v_vitaminas;

  INSERT INTO categorias (nombre, descripcion, estado, fecha_creacion)
  VALUES ('Higiene', 'Productos de Higiene', 1, SYSDATE)
  RETURNING id INTO v_higiene;

  INSERT INTO categorias (nombre, descripcion, estado, fecha_creacion)
  VALUES ('Medicamentos Fuertes', 'Medicamentos Fuertes (Desabilitado Temporalmente)', 0, SYSDATE)
  RETURNING id INTO v_inactiva;

  INSERT INTO clientes
    (dni, nombres, apellidos, email, telefono, direccion, estado, fecha_creacion)
  VALUES ('90000001', 'María Elena', 'Huamán Flores', 'maria.huaman@gmail.com',
          '900000001', 'Por Lurigancho Chosica, Alameda', 1, SYSDATE);
  INSERT INTO clientes
    (dni, nombres, apellidos, email, telefono, direccion, estado, fecha_creacion)
  VALUES ('90000002', 'José Luis', 'Pérez Soto', 'jose.perez@gmail.com',
          '900000002', 'Por Lurigancho Chosica, Betania', 1, SYSDATE);
  INSERT INTO clientes
    (dni, nombres, apellidos, email, telefono, direccion, estado, fecha_creacion)
  VALUES ('90000003', 'Ana Lucía', 'Quispe Díaz', 'ana.quispe@gmail.com',
          NULL, NULL, 1, SYSDATE);
  INSERT INTO clientes
    (dni, nombres, apellidos, email, telefono, direccion, estado, fecha_creacion)
  VALUES ('90000004', 'Carlos', 'Ramírez Vega', 'carlos.ramirez@gmail.com',
          '900000004', 'Por Lurigancho Chosica, cerca de la UPeU', 1, SYSDATE);
  INSERT INTO clientes
    (dni, nombres, apellidos, email, telefono, direccion, estado, fecha_creacion)
  VALUES ('90000005', 'Martín', 'Guevara Salmón', 'martin.quevara@gmail.com',
          NULL, NULL, 0, SYSDATE);

  producto('Paracetamol', 'Paracetamol 500 mg', 2.50, 50, v_analgesicos);
  producto('Ibuprofeno', 'Ibuprofeno 400 mg', 3.75, 30, v_analgesicos);
  producto('Vitamina C', 'Vitamina C 50 mg', 1.20, 40, v_vitaminas);
  producto('Multivitamínico', 'Multivitamínico de Calidad', 12.90, 20, v_vitaminas);
  producto('Alcohol 70%', 'Alcohol 70% 250 ml', 6.80, 25, v_higiene);
  producto('Gel antibacterial', 'Gel antibacterial 10ml (Bajo Stock)',
           4.35, 2, v_higiene);
  producto('Guantes de Latex', 'Guantes de Latex para protección (No Stock)',
           5.00, 0, v_higiene);
  producto('Jabón de mesa', 'Jabón de mesa (Inactivo)',
           7.50, 10, v_higiene, 0);

  COMMIT;
EXCEPTION
  WHEN OTHERS THEN
    ROLLBACK;
    RAISE;
END;

SELECT id, nombre, estado FROM categorias ORDER BY id;
SELECT id, dni, nombres, apellidos, estado FROM clientes ORDER BY id;
SELECT id, nombre, precio, stock, categoria_id, estado FROM productos ORDER BY id;
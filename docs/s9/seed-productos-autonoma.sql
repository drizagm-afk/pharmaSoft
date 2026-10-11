-- Ejecutar una sola vez en la misma base del backend.
-- Puedes cambiar nombres y descripciones. Conserva los precios 12.50 y 18.90
-- para los casos C-01 y C-02, y stocks suficientes para repetir las pruebas.
-- Las categorías se buscan por nombre; los IDs de producto se generan solos.

DECLARE
  v_vitaminas categorias.id%TYPE;
  v_higiene categorias.id%TYPE;
BEGIN
  SAVEPOINT productos_autonoma;

  SELECT id INTO v_vitaminas
  FROM categorias WHERE nombre = 'Vitaminas' AND estado = 1;

  SELECT id INTO v_higiene
  FROM categorias WHERE nombre = 'Higiene' AND estado = 1;

  INSERT INTO productos
    (nombre, descripcion, precio, stock, categoria_id, estado, fecha_creacion)
  VALUES
    ('Vitamina D', 'Vitamina D en presentación de prueba',
     12.50, 30, v_vitaminas, 1, SYSDATE);

  INSERT INTO productos
    (nombre, descripcion, precio, stock, categoria_id, estado, fecha_creacion)
  VALUES
    ('Protector solar', 'Protector solar para el cuidado diario',
     18.90, 25, v_higiene, 1, SYSDATE);

  COMMIT;
EXCEPTION
  WHEN OTHERS THEN
    ROLLBACK TO productos_autonoma;
    RAISE;
END;
/

-- Inventario posterior a la inserción.
SELECT p.id, p.nombre, p.precio, p.stock, c.nombre AS categoria, p.estado
FROM productos p
JOIN categorias c ON c.id = p.categoria_id
ORDER BY p.id;

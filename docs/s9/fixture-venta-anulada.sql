-- Oracle SQL Developer (Run Script / F5) or SQL*Plus.
-- Practice database only. Run during the validation phase, after creating a sale in the app.
-- Select a sale created specifically for the cancelled-receipt printing test.
-- Restores its stock once and marks it ANULADA; no sales or detail rows are deleted.
SET SERVEROUTPUT ON;
ACCEPT venta_id NUMBER PROMPT 'ID de la venta de prueba que desea anular: '

DECLARE
  v_venta_id ventas.id%TYPE := &venta_id;
  v_estado ventas.estado%TYPE;
BEGIN
  SELECT estado INTO v_estado FROM ventas WHERE id = v_venta_id FOR UPDATE;
  IF v_estado = 'REGISTRADA' THEN
    FOR linea IN (
      SELECT producto_id, SUM(cantidad) AS cantidad
      FROM detalle_ventas
      WHERE venta_id = v_venta_id
      GROUP BY producto_id
      ORDER BY producto_id
    ) LOOP
      UPDATE productos
      SET stock = stock + linea.cantidad, fecha_modificacion = LOCALTIMESTAMP
      WHERE id = linea.producto_id;
    END LOOP;
    UPDATE ventas SET estado = 'ANULADA' WHERE id = v_venta_id;
    COMMIT;
    DBMS_OUTPUT.PUT_LINE('Venta de prueba anulada y stock restaurado.');
  ELSIF v_estado = 'ANULADA' THEN
    ROLLBACK;
    DBMS_OUTPUT.PUT_LINE('La venta ya estaba anulada; no se modifico el stock.');
  ELSE
    RAISE_APPLICATION_ERROR(-20001, 'Estado de venta no permitido para esta prueba.');
  END IF;
EXCEPTION
  WHEN NO_DATA_FOUND THEN
    ROLLBACK;
    RAISE_APPLICATION_ERROR(-20002, 'No existe la venta indicada.');
  WHEN OTHERS THEN
    ROLLBACK;
    RAISE;
END;
/
UNDEFINE venta_id;

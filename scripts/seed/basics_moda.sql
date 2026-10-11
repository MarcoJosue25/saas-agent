insert into empresas (nombre, id_numero_meta)
values ('Basics Moda', '100000000000001');

-- Da el último id generado por Mysql
set @empresa_id = last_insert_id();

insert into productos (empresa_id, nombre, descripcion, precio, stock, activo) values
(@empresa_id, 'Polo básico negro', 'Polo de algodón manga corta. Tallas S a XL.', 35.00, 40, true),
(@empresa_id, 'Polo básico blanco', 'Polo de algodón manga corta. Tallas S a XL.', 35.00, 25, true),
(@empresa_id, 'Polo básico azul', 'Polo de algodón manga corta. Tallas S a XL.', 39.90, 18, true),
(@empresa_id, 'Jogger gris', 'Pantalón jogger de algodón. Tallas S a XL.', 79.90, 15, true),
(@empresa_id, 'Jogger negro', 'Pantalón jogger de algodón. Tallas S a XL.', 79.90, 0, true),
(@empresa_id, 'Short azul', 'Short deportivo con bolsillos. Tallas S a L.', 49.90, 20, true),
(@empresa_id, 'Short negro', 'Short deportivo con bolsillos. Tallas S a L.', 49.90, 12, false),
(@empresa_id, 'Polera negra con capucha', 'Polera con capucha y bolsillo. Tallas S a XL.', 99.90, 10, true);


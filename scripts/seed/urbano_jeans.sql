insert into empresas (nombre, id_numero_meta)
values ('Urbano Jeans', '100000000000002');

set @empresa_id = last_insert_id();

-- 'Polo básico negro' para comparar con el de Basics Moda, con otro precio y otro stock
insert into productos (empresa_id, nombre, descripcion, precio, stock, activo) values
(@empresa_id, 'Jean clásico azul', 'Jean de corte recto. Tallas 28 a 36.', 119.90, 22, true),
(@empresa_id, 'Jean negro slim', 'Jean slim elástico. Tallas 28 a 36.', 129.90, 16, true),
(@empresa_id, 'Jean celeste desgastado', 'Jean con lavado desgastado. Tallas 28 a 36.', 139.90, 0, true),
(@empresa_id, 'Casaca de jean', 'Casaca de jean clásica. Tallas S a XL.', 149.90, 9, true),
(@empresa_id, 'Camisa de jean', 'Camisa de jean manga larga. Tallas S a XL.', 89.90, 12, true),
(@empresa_id, 'Bermuda de jean', 'Bermuda de jean para verano. Tallas 28 a 36.', 79.90, 14, true),
(@empresa_id, 'Polo básico negro', 'Polo de algodón manga corta. Tallas S a XL.', 29.90, 30, true),
(@empresa_id, 'Polo estampado', 'Polo con estampado urbano. Tallas S a XL.', 45.00, 20, true),
(@empresa_id, 'Cinturón de cuero', 'Cinturón de cuero negro. Talla única.', 59.90, 15, false);

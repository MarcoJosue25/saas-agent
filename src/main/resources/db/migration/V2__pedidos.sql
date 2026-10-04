create table pedido (
    id bigint primary key auto_increment,
    empresa_id bigint not null,
    conversacion_id bigint not null,
    total decimal(10,2) not null,
    fecha_creacion datetime not null default current_timestamp,
    foreign key (empresa_id) references empresa(id),
    foreign key (conversacion_id) references conversacion(id)
);

create table pedido_item (
    id bigint primary key auto_increment,
    pedido_id bigint not null,
    producto_id bigint not null,
    cantidad int not null,
    talla varchar(20),
    precio_unitario decimal(10,2) not null,
    foreign key (pedido_id) references pedido(id),
    foreign key (producto_id) references producto(id)
);
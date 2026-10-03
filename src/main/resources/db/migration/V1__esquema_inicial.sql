create table empresa (
    id bigint primary key auto_increment,
    nombre varchar(255) not null,
    id_numero_meta varchar(50) not null unique,
    activo boolean not null default true,
    fecha_creacion datetime not null default current_timestamp
);

create table conversacion (
    id bigint primary key auto_increment,
    empresa_id bigint not null,
    telefono_cliente varchar(20) not null,
    fecha_ultimo_mensaje datetime not null default current_timestamp,
    fecha_creacion datetime not null default current_timestamp,
    unique (empresa_id, telefono_cliente),
    foreign key (empresa_id) references empresa(id)
);

create table mensaje (
    id bigint primary key auto_increment,
    conversacion_id bigint not null,
    rol varchar(20) not null,
    contenido text not null,
    id_mensaje_meta varchar(255) unique,
    fecha_creacion datetime not null default current_timestamp,
    foreign key (conversacion_id) references conversacion(id)
);

create table producto (
    id bigint primary key auto_increment,
    empresa_id bigint not null,
    nombre varchar(255) not null,
    descripcion varchar(255),
    precio decimal(10,2) not null,
    stock int not null default 0,
    activo boolean not null default true,
    fecha_creacion datetime not null default current_timestamp,
    foreign key (empresa_id) references empresa(id)
);

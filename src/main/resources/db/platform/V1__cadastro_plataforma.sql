create table platform_admin (
    login varchar(120) primary key,
    senha varchar(255) not null
);
create table platform_empresa (
    id uuid primary key,
    nome varchar(150) not null,
    responsavel varchar(150) not null,
    slug varchar(40) not null unique,
    db_name varchar(63) not null unique,
    db_user varchar(63) not null unique,
    db_password text not null,
    estado varchar(20) not null check (estado in ('CRIANDO','ATIVA','SUSPENSA','FALHOU')),
    criado_em timestamp with time zone not null default current_timestamp
);

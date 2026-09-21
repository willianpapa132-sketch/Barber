create table app_usuario (
    id bigserial primary key,
    login varchar(80) not null unique,
    senha_hash varchar(255) not null,
    nome varchar(120) not null,
    perfil varchar(20) not null,
    ativo boolean not null default true,
    criado_em timestamp not null default now(),
    atualizado_em timestamp not null default now()
);

create table configuracao_barbearia (
    id bigint primary key,
    nome varchar(160) not null,
    telefone varchar(30),
    endereco varchar(255),
    min_antecedencia_minutos integer not null,
    horizonte_dias integer not null
);

create table horario_funcionamento (
    id bigserial primary key,
    dia_semana integer not null,
    hora_abertura time not null,
    hora_fechamento time not null,
    fechado boolean not null default false,
    unique(dia_semana)
);

create table servico (
    id bigserial primary key,
    nome varchar(120) not null,
    descricao varchar(600),
    preco numeric(12,2) not null,
    duracao_minutos integer not null,
    ativo boolean not null default true,
    criado_em timestamp not null default now(),
    atualizado_em timestamp not null default now(),
    constraint ck_servico_preco check (preco >= 0),
    constraint ck_servico_duracao check (duracao_minutos > 0)
);

create table barbeiro (
    id bigserial primary key,
    nome varchar(120) not null,
    telefone varchar(30),
    ativo boolean not null default true,
    usuario_id bigint not null unique references app_usuario(id),
    versao bigint not null default 0
);

create table barbeiro_servico (
    barbeiro_id bigint not null references barbeiro(id),
    servico_id bigint not null references servico(id),
    primary key (barbeiro_id, servico_id)
);

create table jornada_barbeiro (
    id bigserial primary key,
    barbeiro_id bigint not null references barbeiro(id),
    dia_semana integer not null,
    hora_inicio time not null,
    hora_fim time not null,
    intervalo_inicio time,
    intervalo_fim time,
    ativo boolean not null default true,
    unique(barbeiro_id, dia_semana)
);

create table bloqueio_barbeiro (
    id bigserial primary key,
    barbeiro_id bigint not null references barbeiro(id),
    data_bloqueio date not null,
    hora_inicio time,
    hora_fim time,
    motivo varchar(255),
    criado_em timestamp not null default now()
);

create table cliente (
    id bigserial primary key,
    nome varchar(120) not null,
    telefone varchar(30) not null,
    telefone_normalizado varchar(20) not null,
    criado_em timestamp not null default now(),
    atualizado_em timestamp not null default now()
);
create index idx_cliente_busca on cliente (telefone_normalizado, nome);

create table agendamento (
    id bigserial primary key,
    codigo_confirmacao varchar(32) not null unique,
    cliente_id bigint references cliente(id),
    barbeiro_id bigint not null references barbeiro(id),
    servico_id bigint not null references servico(id),
    nome_cliente varchar(120) not null,
    telefone_cliente varchar(30) not null,
    nome_servico_snapshot varchar(120) not null,
    preco_servico_snapshot numeric(12,2) not null,
    duracao_servico_minutos_snapshot integer not null,
    inicio_em timestamp not null,
    fim_em timestamp not null,
    status varchar(30) not null,
    pagamento_recebido boolean not null default false,
    criado_por_usuario_id bigint references app_usuario(id),
    criado_em timestamp not null default now(),
    atualizado_em timestamp not null default now(),
    motivo_cancelamento varchar(255)
);
create index idx_agendamento_barbeiro_horario on agendamento (barbeiro_id, inicio_em, fim_em);
create index idx_agendamento_status on agendamento (status);

create table sessao_caixa (
    id bigserial primary key,
    aberto_em timestamp not null default now(),
    fechado_em timestamp,
    aberto_por_usuario_id bigint not null references app_usuario(id),
    fechado_por_usuario_id bigint references app_usuario(id),
    dinheiro_inicial numeric(12,2) not null,
    dinheiro_esperado numeric(12,2),
    dinheiro_contado numeric(12,2),
    diferenca_dinheiro numeric(12,2),
    status varchar(20) not null
);
create unique index ux_sessao_caixa_uma_aberta on sessao_caixa(status) where status = 'ABERTO';

create table movimento_caixa (
    id bigserial primary key,
    sessao_caixa_id bigint not null references sessao_caixa(id),
    agendamento_id bigint references agendamento(id),
    movimento_original_id bigint references movimento_caixa(id),
    tipo varchar(30) not null,
    forma_pagamento varchar(30),
    valor numeric(12,2) not null,
    descricao varchar(255) not null,
    categoria varchar(80),
    criado_por_usuario_id bigint not null references app_usuario(id),
    criado_em timestamp not null default now(),
    motivo_estorno varchar(255),
    estornado boolean not null default false
);
create unique index ux_um_recebimento_ativo_por_agendamento
    on movimento_caixa(agendamento_id)
    where tipo = 'RECEBIMENTO_SERVICO' and estornado = false;

insert into configuracao_barbearia(id, nome, telefone, endereco, min_antecedencia_minutos, horizonte_dias)
values (1, 'Barbearia', '', '', 30, 60);

insert into horario_funcionamento(dia_semana, hora_abertura, hora_fechamento, fechado) values
(1, '09:00', '18:00', false),
(2, '09:00', '18:00', false),
(3, '09:00', '18:00', false),
(4, '09:00', '18:00', false),
(5, '09:00', '18:00', false),
(6, '09:00', '14:00', false),
(7, '09:00', '14:00', true);

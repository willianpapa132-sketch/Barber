create table configuracao_plano_mensal (
    id bigserial primary key,
    barbeiro_id bigint not null unique references barbeiro(id),
    valor_mensal numeric(12,2) not null,
    cortes_por_mes integer not null,
    cortes_por_semana integer not null,
    ativo boolean not null default true,
    atualizado_em timestamp not null default now(),
    constraint ck_config_plano_mensal_valor check (valor_mensal >= 0),
    constraint ck_config_plano_mensal_mes check (cortes_por_mes > 0),
    constraint ck_config_plano_mensal_semana check (cortes_por_semana > 0 and cortes_por_semana <= cortes_por_mes)
);

create table plano_mensal_cliente (
    id bigserial primary key,
    cliente_id bigint not null references cliente(id),
    barbeiro_id bigint not null references barbeiro(id),
    valor_mensal_snapshot numeric(12,2) not null,
    cortes_por_mes_snapshot integer not null,
    cortes_por_semana_snapshot integer not null,
    data_inicio date not null,
    data_fim date,
    ativo boolean not null default true,
    criado_em timestamp not null default now(),
    atualizado_em timestamp not null default now(),
    constraint ck_plano_mensal_cliente_valor check (valor_mensal_snapshot >= 0),
    constraint ck_plano_mensal_cliente_mes check (cortes_por_mes_snapshot > 0),
    constraint ck_plano_mensal_cliente_semana check (cortes_por_semana_snapshot > 0 and cortes_por_semana_snapshot <= cortes_por_mes_snapshot)
);

create index idx_plano_mensal_cliente_lookup on plano_mensal_cliente (barbeiro_id, ativo, data_inicio, data_fim);
create unique index ux_plano_mensal_cliente_ativo
    on plano_mensal_cliente(cliente_id, barbeiro_id)
    where ativo = true;

alter table agendamento add column plano_mensal_id bigint references plano_mensal_cliente(id);
create index idx_agendamento_plano_mensal_horario on agendamento (plano_mensal_id, inicio_em);

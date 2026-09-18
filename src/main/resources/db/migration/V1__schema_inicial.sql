create table app_user (
    id bigserial primary key,
    username varchar(80) not null unique,
    password_hash varchar(255) not null,
    nome varchar(120) not null,
    role varchar(20) not null,
    active boolean not null default true,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now()
);

create table barber_shop_config (
    id bigint primary key,
    nome varchar(160) not null,
    telefone varchar(30),
    endereco varchar(255),
    min_antecedencia_minutos integer not null,
    horizonte_dias integer not null
);

create table shop_hours (
    id bigserial primary key,
    day_of_week integer not null,
    open_time time not null,
    close_time time not null,
    closed boolean not null default false,
    unique(day_of_week)
);

create table service_catalog (
    id bigserial primary key,
    nome varchar(120) not null,
    descricao varchar(600),
    preco numeric(12,2) not null,
    duracao_minutos integer not null,
    active boolean not null default true,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    constraint ck_service_price check (preco >= 0),
    constraint ck_service_duration check (duracao_minutos > 0)
);

create table barber (
    id bigserial primary key,
    nome varchar(120) not null,
    telefone varchar(30),
    active boolean not null default true,
    user_id bigint not null unique references app_user(id),
    version bigint not null default 0
);

create table barber_service (
    barber_id bigint not null references barber(id),
    service_id bigint not null references service_catalog(id),
    primary key (barber_id, service_id)
);

create table barber_work_schedule (
    id bigserial primary key,
    barber_id bigint not null references barber(id),
    day_of_week integer not null,
    start_time time not null,
    end_time time not null,
    break_start time,
    break_end time,
    active boolean not null default true,
    unique(barber_id, day_of_week)
);

create table barber_block (
    id bigserial primary key,
    barber_id bigint not null references barber(id),
    block_date date not null,
    start_time time,
    end_time time,
    reason varchar(255),
    created_at timestamp not null default now()
);

create table customer (
    id bigserial primary key,
    nome varchar(120) not null,
    telefone varchar(30) not null,
    telefone_normalizado varchar(20) not null,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now()
);
create index idx_customer_search on customer (telefone_normalizado, nome);

create table appointment (
    id bigserial primary key,
    confirmation_code varchar(32) not null unique,
    customer_id bigint references customer(id),
    barber_id bigint not null references barber(id),
    service_id bigint not null references service_catalog(id),
    customer_name varchar(120) not null,
    customer_phone varchar(30) not null,
    service_name_snapshot varchar(120) not null,
    service_price_snapshot numeric(12,2) not null,
    service_duration_minutes_snapshot integer not null,
    start_at timestamp not null,
    end_at timestamp not null,
    status varchar(30) not null,
    payment_received boolean not null default false,
    created_by_user_id bigint references app_user(id),
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    cancelled_reason varchar(255)
);
create index idx_appointment_barber_time on appointment (barber_id, start_at, end_at);
create index idx_appointment_status on appointment (status);

create table cash_session (
    id bigserial primary key,
    opened_at timestamp not null default now(),
    closed_at timestamp,
    opened_by_user_id bigint not null references app_user(id),
    closed_by_user_id bigint references app_user(id),
    initial_cash numeric(12,2) not null,
    expected_cash numeric(12,2),
    counted_cash numeric(12,2),
    difference_cash numeric(12,2),
    status varchar(20) not null
);
create unique index ux_cash_session_one_open on cash_session(status) where status = 'ABERTO';

create table cash_movement (
    id bigserial primary key,
    cash_session_id bigint not null references cash_session(id),
    appointment_id bigint references appointment(id),
    original_movement_id bigint references cash_movement(id),
    type varchar(30) not null,
    payment_method varchar(30),
    amount numeric(12,2) not null,
    description varchar(255) not null,
    category varchar(80),
    created_by_user_id bigint not null references app_user(id),
    created_at timestamp not null default now(),
    reversal_reason varchar(255),
    reversed boolean not null default false
);
create unique index ux_one_active_receipt_per_appointment
    on cash_movement(appointment_id)
    where type = 'RECEBIMENTO_SERVICO' and reversed = false;

insert into barber_shop_config(id, nome, telefone, endereco, min_antecedencia_minutos, horizonte_dias)
values (1, 'Barbearia', '', '', 30, 60);

insert into shop_hours(day_of_week, open_time, close_time, closed) values
(1, '09:00', '18:00', false),
(2, '09:00', '18:00', false),
(3, '09:00', '18:00', false),
(4, '09:00', '18:00', false),
(5, '09:00', '18:00', false),
(6, '09:00', '14:00', false),
(7, '09:00', '14:00', true);

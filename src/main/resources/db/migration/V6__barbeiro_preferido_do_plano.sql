-- Preferencia opcional; contratos existentes continuam sem barbeiro definido.
alter table plano_mensal_cliente add column barbeiro_preferido_id bigint;
alter table plano_mensal_cliente add constraint fk_plano_barbeiro_preferido
    foreign key (barbeiro_preferido_id) references barbeiro(id);
create index ix_plano_barbeiro_preferido on plano_mensal_cliente(barbeiro_preferido_id);

drop trigger trg_plano_mensal_cliente_empresa on plano_mensal_cliente;
create trigger trg_plano_mensal_cliente_empresa before insert or update on plano_mensal_cliente
    for each row execute function barber_validar_vinculos_empresa(
        'cliente','cliente_id','usuario','usuario_id','plano_mensal','plano_mensal_id',
        'barbeiro','barbeiro_preferido_id');

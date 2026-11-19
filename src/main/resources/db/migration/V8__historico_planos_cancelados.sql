-- Permite uma nova contratacao sem substituir pagamentos e atendimentos antigos.
do $$
declare restricao record;
begin
    for restricao in
        select conname from pg_constraint
        where conrelid='plano_mensal_cliente'::regclass and contype='u'
          and pg_get_constraintdef(oid)='UNIQUE (usuario_id)'
    loop
        execute format('alter table plano_mensal_cliente drop constraint %I',restricao.conname);
    end loop;
end $$;

create unique index uk_plano_usuario_ativo on plano_mensal_cliente(empresa_id,usuario_id) where ativo=true;
create unique index uk_plano_cliente_ativo on plano_mensal_cliente(empresa_id,cliente_id) where ativo=true;

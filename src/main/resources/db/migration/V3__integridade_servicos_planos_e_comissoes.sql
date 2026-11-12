-- Consolida as regras que ainda faltavam dos antigos scripts de docs/sql.
-- Tabelas, colunas e demais relacionamentos ja foram criados pela V1.
-- O Flyway controla a transacao; dados historicos nao sao recalculados.

alter table agendamento_servicos_plano
    add constraint ck_agendamento_plano_duracao check (duracao_minutos > 0),
    add constraint ck_agendamento_plano_preco_original check (preco_original >= 0),
    add constraint ck_agendamento_plano_desconto check (porcentagem_desconto between 0 and 100),
    add constraint ck_agendamento_plano_preco_liquido check (preco_liquido >= 0),
    add constraint fk_agendamento_plano_servico foreign key (servico_id) references servico(id);

create index ix_comissao_plano_periodo on comissao_plano (plano_mensal_id, data_agendamento);

# Servicos extras em planos

Os servicos de `PlanosMensal.servicosIncluidos` nao sao cobrados e geram um ponto por minuto. Os demais servicos sao cobrados pelo preco normal, ou pelo preco com desconto quando estiverem em uma regra `ServicosPlanosDesconto` daquele plano. Um desconto de 100% nao transforma o extra em servico incluido e nao gera pontos.

Cada regra tem `planoMensal`, `porcentagemDesconto` (inteiro de 0 a 100) e um `Set<Servico> servicos`. Um servico extra pode aparecer em uma unica regra por plano. Servicos incluidos tem prioridade sobre regras de desconto.

Os requests de criar/atualizar plano aceitam o campo opcional:

```json
{
  "descontosServicos": [
    { "porcentagemDesconto": 20, "servicosIds": [5, 6] },
    { "porcentagemDesconto": 10, "servicosIds": [7] }
  ]
}
```

Na atualizacao, omitir o campo preserva os descontos; enviar uma lista vazia remove as regras. Os IDs sao exemplos e devem corresponder aos servicos cadastrados. Nao foi adicionada uma tela administrativa de descontos.

O agendamento guarda o detalhamento dos precos, descontos, cobertura e minutos em `agendamento_servicos_plano`. Alterar o plano ou o catalogo depois de agendar nao recalcula essas condicoes. O horario considera a duracao de todos os servicos, mas so os incluidos geram pontos.

No fechamento da comanda pelo administrador, os pontos e a comissao dos extras sao registrados na mesma transacao. A comissao usa o percentual do barbeiro sobre a soma dos precos liquidos dos extras, com arredondamento monetario por servico. Quando ha valor a cobrar, e obrigatorio informar dinheiro, Pix, debito ou credito. Comandas sem cobranca usam lista vazia de pagamentos; `MENSAL` nao e uma entrada financeira.

Exemplo: corte incluido de 30 minutos + extra de R$ 50 com 20% de desconto + extra de R$ 10 sem desconto = 30 pontos e R$ 50 a cobrar. Se o barbeiro tem 40% de comissao, recebe R$ 20 em comissao.

O esquema e atualizado automaticamente pelo Flyway em `src/main/resources/db/migration`: a V1 cria as tabelas, a V2 aplica o isolamento por empresa e a V3 completa as validacoes do detalhamento e o indice de comissoes. Os antigos scripts manuais de `docs/sql` foram consolidados nessas migrations e removidos. A V3 valida os registros existentes sem recalcular valores; registros invalidos devem ser corrigidos explicitamente antes de reaplicar a migration. Para bancos legados, consulte [Migrations](MIGRATIONS.md).

Agendamentos antigos sem detalhamento usam o plano/catalogo disponivel ao finalizar; quando integralmente cobertos, os pontos preservam a duracao total registrada no agendamento.

O fluxo atual de fechamento, com produtos, descontos adicionais e pagamentos divididos, esta em [Comandas](COMANDAS.md). A comissao passa a considerar o valor liquido de cada item da comanda e seus percentuais registrados.

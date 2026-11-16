# Fechamento de comandas

O administrador abre a comanda pela agenda, em `/admin/agendamentos/{id}/comanda`.
A tela usa o visual atual do painel. O barbeiro continua consultando sua agenda,
carteira e extratos; pode registrar cancelamento ou falta, mas nao pode fechar
comanda nem registrar recebimentos. As antigas rotas de finalizacao foram bloqueadas.

## Fluxo e regras

- A previa usa precos, cobertura de planos, estoque e percentuais do servidor.
  O navegador informa somente IDs, quantidades, desconto, observacao e pagamentos.
  Nao informa empresa nem precos/comissoes confiaveis.
- A selecao inicial contem os servicos agendados. O admin pode remover/adicionar
  servicos, adicionar produtos e ajustar quantidades. A agenda original e mantida;
  o detalhamento efetivamente vendido fica na comanda.
- A previa nao reserva estoque nem grava uma comanda aberta. O fechamento e uma
  unica transacao, permitido a partir do inicio do atendimento. Cada agendamento
  pode gerar uma unica comanda. Comanda fechada nao possui rota de edicao/reabertura.
- Desconto pode ser nenhum, valor em reais ou percentual ate 100%. Ele e rateado
  proporcionalmente entre os itens cobrados, com distribuicao deterministica dos
  centavos restantes. A soma dos descontos/itens sempre fecha com o total.
- Servicos usam o percentual do barbeiro; produtos usam `percentualComissao` do
  cadastro do produto (0% por padrao nos produtos existentes). A comissao de cada
  item e calculada sobre seu valor liquido e arredondada em centavos. O total e
  a soma das comissoes dos itens, incluindo produtos.
- Servicos cobertos por plano possuem valor a cobrar zero e geram pontos por
  minuto. As condicoes de servicos ja agendados com plano sao preservadas pelo
  snapshot existente. Extras adicionados usam as regras atuais do plano.
  Produtos nao sao cobertos por planos. Planos bloqueados/inativos precisam ser
  regularizados antes de fechar uma comanda que utiliza sua cobertura.
- Pagamentos aceitos: dinheiro, Pix, debito e credito, inclusive combinados.
  Valores devem ser positivos e a soma precisa ser exatamente o total. Nao ha
  troco automatico, saldo devedor nem pagamento ficticio `MENSAL`. Se o total for
  zero, a lista de pagamentos deve ficar vazia.
- Comandas com valor exigem caixa aberto. O caixa recebe um unico movimento pelo
  total, vinculado a comanda, com acesso ao detalhamento dos pagamentos. Estoque,
  comanda, comissoes, pontos e caixa sao confirmados juntos. Qualquer falha desfaz
  todas essas operacoes. Locks no agendamento, produtos e caixa protegem contra
  fechamento duplicado, venda sem estoque e perda de atualizacoes concorrentes.

## Relatorios e historico

`Comanda` e a fonte financeira. Precos, nomes, percentuais, descontos, comissoes,
pontos e pagamentos ficam congelados no fechamento. Editar o catalogo depois nao
altera uma venda ja fechada. O percentual efetivo agregado de uma comissao mista
nao substitui os percentuais individuais dos itens.

Financeiro, receita e despesas do dashboard, home administrativa, carteira,
comissoes e extratos usam a data de fechamento da comanda. O financeiro mostra
cada parcela recebida separadamente. Comandas sem cobranca nao geram receita,
mas continuam contabilizando fechamento e eventuais pontos/baixa de estoque.

Contagens/grafico de agendamentos continuam representando a agenda pela data
marcada. O indicador de comandas fechadas e independente dessa contagem: uma
comanda de um agendamento antigo pode ser fechada hoje. Recebimentos de
mensalidades continuam sendo uma fonte separada de receita, pela data do pagamento;
nao sao novamente cobrados pelos servicos incluidos na comanda.

As tabelas de comissao continuam como registros derivados, vinculados a comanda.
Campos de pagamento no agendamento sao mantidos para compatibilidade de exibicao,
mas nao alimentam mais os calculos financeiros.

## API

- `GET /api/admin/comandas/catalogo`: servicos e produtos da empresa.
- `GET /api/admin/comandas/agendamento/{id}`: selecao inicial ou comprovante fechado.
- `POST /api/admin/comandas/agendamento/{id}/previa`: calculo sem persistencia.
- `POST /api/admin/comandas/agendamento/{id}/fechar`: fechamento atomico.

Exemplo de corpo:

```json
{
  "itens": [
    { "tipo": "SERVICO", "id": 1, "quantidade": 1 },
    { "tipo": "PRODUTO", "id": 2, "quantidade": 2 }
  ],
  "tipoDesconto": "PERCENTUAL",
  "desconto": 10,
  "observacao": "Fidelidade",
  "pagamentos": [
    { "forma": "PIX", "valor": 80 },
    { "forma": "CREDITO", "valor": 100 }
  ]
}
```

Os IDs e valores sao ilustrativos. So ADMIN pode acessar a API e o service de
comandas. O escopo continua sendo a empresa resolvida pelo host/subdominio.

## Migration V5

`V5__fechamento_comandas.sql` adiciona percentual de produto, tabelas da comanda,
vinculos financeiros, indices e validacoes de empresa e totais. V1-V4 permanecem
intactas. A migration importa finalizacoes pagas concluidas como comandas legadas,
com os valores, comissoes, pontos e pagamentos originais; nao recalcula catalogo,
nao baixa estoque antigo e nao duplica movimentos de caixa.

Como o modelo anterior nao registrava toda a composicao da venda, o comprovante
legado contem uma linha agregada identificada como historico. O detalhamento
antigo permanece nas tabelas originais. Pagamentos cuja forma nao foi registrada
recebem o marcador `LEGADO`, nunca selecionavel em novas vendas.

Comissoes/entradas sem uma finalizacao paga concluida e datas/valores historicos
inconsistentes devem ser regularizados explicitamente antes de migrar. A V5
aborta transacionalmente nessas situacoes, preservando os dados para revisao.

## Validacao

`ComandaIntegrationTest` cobre desconto, pagamentos divididos, comissoes de
produtos/servicos, estoque, isolamento por empresa, permissoes, plano sem receita,
rollback sem caixa e fechamento concorrente. A suite pode usar PostgreSQL vazio
com `COMANDA_TEST_DB_URL`; os testes de migrations usam `MIGRATION_TEST_DB_URL`.

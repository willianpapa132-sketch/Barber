# Documentação Técnica

Este documento descreve a estrutura técnica do Sistema de Barbearia, as principais classes, regras de negócio, migrations e telas.

## Visão Geral

O projeto é uma aplicação Spring Boot MVC com Thymeleaf, Spring Security, Spring Data JPA, Flyway e PostgreSQL.

Camadas principais:

- `config`: configuração da aplicação, segurança e carga inicial.
- `domain`: entidades JPA e enums.
- `repository`: interfaces Spring Data JPA.
- `service`: regras de negócio transacionais.
- `web`: controllers MVC.
- `web.form`: DTOs de formulário.
- `templates`: páginas Thymeleaf.
- `static`: CSS e outros arquivos estáticos.

Classe principal:

```text
bruninho.Barbeiro.BarbeiroApplication
```

## Configuração

### `AppProperties`

Lê propriedades do prefixo `app`.

Campos principais:

- `app.zone`: fuso horário da aplicação.
- `app.demo-data`: cria ou não dados demonstrativos.
- `app.initial-admin.*`: dados do administrador inicial.

### `SecurityConfig`

Configura autenticação por sessão, BCrypt, CSRF, logout e permissões.

Regras de acesso:

- Público: `/`, `/agendar/**`, `/planos/**`, `/css/**`, `/js/**`, `/login`.
- Administrador: `/admin/**`.
- Barbeiro: `/barbeiro/**`.
- Demais rotas exigem autenticação.

### `DataInitializer`

Executa na inicialização.

Responsabilidades:

- Garante uma linha em `configuracao_barbearia`.
- Garante horários padrão em `horario_funcionamento`.
- Cria administrador inicial se configurado e ainda inexistente.
- Cria dados demonstrativos quando `APP_DEMO_DATA=true`.

## Entidades

### `Usuario`

Tabela: `app_usuario`.

Representa usuários autenticáveis do sistema.

Campos principais:

- `login`
- `senhaHash`
- `nome`
- `perfil`
- `ativo`
- `criadoEm`
- `atualizadoEm`

### `Perfil`

Enum de autorização:

- `ADMIN`
- `BARBEIRO`

### `Servico`

Tabela: `servico`.

Representa um serviço oferecido pela barbearia.

Campos principais:

- `nome`
- `descricao`
- `preco`
- `duracaoMinutos`
- `ativo`

Serviços são desativados em vez de excluídos fisicamente no fluxo normal, preservando histórico.

### `Barbeiro`

Tabela: `barbeiro`.

Representa o profissional que executa serviços.

Campos principais:

- `nome`
- `telefone`
- `ativo`
- `usuario`
- `versao`
- `servicos`

Relacionamento de serviços: tabela `barbeiro_servico`.

### `ConfiguracaoBarbearia`

Tabela: `configuracao_barbearia`.

Define parâmetros globais:

- `nome`
- `telefone`
- `endereco`
- `minAntecedenciaMinutos`
- `horizonteDias`

### `HorarioFuncionamento`

Tabela: `horario_funcionamento`.

Define abertura e fechamento da barbearia por dia da semana.

### `JornadaBarbeiro`

Tabela: `jornada_barbeiro`.

Define a jornada semanal do barbeiro, incluindo intervalo.

### `BloqueioBarbeiro`

Tabela: `bloqueio_barbeiro`.

Representa folgas, bloqueios ou indisponibilidades em uma data.

### `Cliente`

Tabela: `cliente`.

Representa um cliente cadastrado.

Campos principais:

- `nome`
- `telefone`
- `telefoneNormalizado`

O fluxo público de agendamento não sobrescreve automaticamente um cliente existente por telefone.

### `Agendamento`

Tabela: `agendamento`.

Representa um atendimento reservado.

Campos principais:

- `codigoConfirmacao`
- `cliente`
- `barbeiro`
- `servico`
- `planoMensal`
- `nomeCliente`
- `telefoneCliente`
- `nomeServicoSnapshot`
- `precoServicoSnapshot`
- `duracaoServicoMinutosSnapshot`
- `inicioEm`
- `fimEm`
- `status`
- `pagamentoRecebido`

O agendamento grava snapshot do serviço para preservar o histórico se o catálogo for alterado.

### `StatusAgendamento`

Valores:

- `AGENDADO`
- `CONCLUIDO`
- `CANCELADO`
- `NAO_COMPARECEU`

### `SessaoCaixa`

Tabela: `sessao_caixa`.

Representa abertura e fechamento de caixa.

Campos principais:

- `abertoEm`
- `fechadoEm`
- `abertoPorUsuario`
- `fechadoPorUsuario`
- `dinheiroInicial`
- `dinheiroEsperado`
- `dinheiroContado`
- `diferencaDinheiro`
- `status`

Só pode existir uma sessão de caixa aberta por vez.

### `MovimentoCaixa`

Tabela: `movimento_caixa`.

Representa entradas, saídas, recebimentos e estornos.

Campos principais:

- `sessaoCaixa`
- `agendamento`
- `movimentoOriginal`
- `tipo`
- `formaPagamento`
- `valor`
- `descricao`
- `categoria`
- `criadoPorUsuario`
- `estornado`

### `ConfiguracaoPlanoMensal`

Tabela: `configuracao_plano_mensal`.

Define o plano mensal de cada barbeiro:

- `valorMensal`
- `cortesPorMes`
- `cortesPorSemana`
- `ativo`

### `PlanoMensalCliente`

Tabela: `plano_mensal_cliente`.

Representa a adesão de um cliente a um plano mensal de um barbeiro.

## Repositórios

Principais repositórios:

- `UsuarioRepositorio`
- `ServicoRepositorio`
- `BarbeiroRepositorio`
- `ClienteRepositorio`
- `AgendamentoRepositorio`
- `JornadaBarbeiroRepositorio`
- `BloqueioBarbeiroRepositorio`
- `SessaoCaixaRepositorio`
- `MovimentoCaixaRepositorio`
- `ConfiguracaoBarbeariaRepositorio`
- `ConfiguracaoPlanoMensalRepositorio`
- `PlanoMensalClienteRepositorio`
- `HorarioFuncionamentoRepositorio`

As consultas customizadas usam JPQL sobre as entidades em português.

## Serviços

### `AdminCatalogoServico`

Centraliza gravações administrativas de serviços, barbeiros, jornadas padrão e clientes.

### `AgendamentoServico`

Cria agendamentos públicos e manuais, remarca horários e troca status.

Regras:

- Valida telefone.
- Bloqueia pessimisticamente o barbeiro antes da gravação.
- Valida disponibilidade.
- Associa plano mensal ativo quando aplicável.
- Gera código de confirmação.

### `DisponibilidadeServico`

Calcula horários disponíveis e valida conflitos.

Considera:

- Horário de funcionamento.
- Jornada do barbeiro.
- Intervalo.
- Bloqueios.
- Duração do serviço.
- Agendamentos existentes.
- Antecedência mínima.
- Horizonte máximo de agenda.

### `CaixaServico`

Controla abertura, fechamento, recebimentos, movimentações manuais e estornos.

### `PlanoMensalServico`

Controla configuração e adesão de planos mensais, além do limite de uso semanal e mensal.

### `UsuarioAtualServico`

Resolve o usuário autenticado atual.

### `NormalizadorTelefone`

Valida e normaliza telefones.

## Controllers

### `InicioControlador`

Entrega a página pública inicial.

### `AgendamentoPublicoControlador`

Fluxo público de agendamento.

### `PlanoMensalPublicoControlador`

Fluxo público de adesão a planos mensais.

### `PainelAdminControlador`

Painel administrativo: dashboard, serviços, barbeiros, clientes e agenda.

### `AdminConfiguracoesControlador`

Configurações da barbearia, jornada, bloqueios e mensalidades.

### `CaixaControlador`

Telas e ações do caixa.

### `AreaBarbeiroControlador`

Agenda do barbeiro autenticado.

## Telas

Públicas:

- `public/home.html`
- `public/agendar.html`
- `public/confirmado.html`
- `public/planos.html`
- `public/plano-confirmado.html`

Admin:

- `admin/dashboard.html`
- `admin/servicos.html`
- `admin/barbeiros.html`
- `admin/clientes.html`
- `admin/agenda.html`
- `admin/caixa.html`
- `admin/relatorios.html`
- `admin/configuracoes.html`
- `admin/mensalidades.html`

Barbeiro:

- `barbeiro/agenda.html`

Autenticação:

- `login.html`

## Banco de Dados

Migrations:

- `V1__schema_inicial.sql`
- `V2__planos_mensais.sql`

Tabelas principais:

- `app_usuario`
- `configuracao_barbearia`
- `horario_funcionamento`
- `servico`
- `barbeiro`
- `barbeiro_servico`
- `jornada_barbeiro`
- `bloqueio_barbeiro`
- `cliente`
- `agendamento`
- `sessao_caixa`
- `movimento_caixa`
- `configuracao_plano_mensal`
- `plano_mensal_cliente`

O Hibernate usa `ddl-auto=validate` no perfil principal. Em produção, o schema deve ser criado por Flyway.

## Perfis

### Padrão

Usa PostgreSQL, Flyway habilitado e validação do schema.

### `dev`

Usa H2 em arquivo local:

```text
./data/barbearia-dev
```

Esse perfil usa `ddl-auto=update` e dados demonstrativos por padrão.

### `test`

Usa H2 em memória com `ddl-auto=create-drop`.

## Testes

Comando:

```powershell
.\mvnw.cmd test
```

Coberturas principais:

- Inicialização do contexto.
- Regras de agendamento e disponibilidade.
- Segurança das rotas.

## Observações de Produção

- Usar PostgreSQL com Flyway.
- Configurar HTTPS.
- Usar senhas fortes e variáveis de ambiente.
- Não versionar credenciais reais.
- Configurar backup e restauração do banco.
- Monitorar logs sem expor dados sensíveis.

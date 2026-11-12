# Empresas em um unico PostgreSQL

A aplicacao usa somente o DataSource configurado por `spring.datasource.*`.
`platform_empresa` e o cadastro principal, mapeado por `Empresa`. Seu UUID foi
preservado para nao alterar identificadores existentes. `estado=ATIVA` libera
acesso; `SUSPENSA`, `CRIANDO` e `FALHOU` bloqueiam acesso. Nao existem mais
credenciais, databases, migrations ou pools separados por empresa.

## Execucao local

```powershell
.\scripts\Initialize-Platform.ps1
docker compose up -d --build
```

O script cria `.env` com login e senha do proprietario. Nao sobrescreve arquivos
existentes. Acesse `http://localhost:8080/plataforma`. Cadastre uma empresa, por
exemplo `bruninho`, e acesse `http://bruninho.localhost:8080`. Configure os hosts
localmente se seu navegador nao resolver subdominios de localhost.

O cadastro grava empresa, administrador, configuracao e sete dias de funcionamento
em uma unica transacao. Uma falha desfaz todos os inserts. O mesmo login pode
existir em empresas diferentes. O administrador da plataforma usa tabela e papel
separados (`platform_admin`, `PLATFORM_ADMIN`).

## Contexto e isolamento

`TenantRequestFilter` usa apenas `request.getServerName()`, valida o dominio,
resolve o slug, consulta o cadastro e guarda o UUID em `TenantContext`. O contexto
e limpo em `finally`, inclusive em rejeicoes e excecoes. Sessao autenticada fica
vinculada ao host/empresa e nao pode ser reaproveitada em outra empresa.
`empresaId` em query string ou JSON nao define contexto nem propriedade de dados.

As dez entidades principais herdam `EmpresaScopedEntity`, com `empresa_id` e
relacao lazy obrigatoria para `Empresa`: Usuario, Cliente, Barbeiro, Servico,
Agendamento, PlanosMensal, PlanoMensalCliente, Caixa, HorarioFuncionamento e
ConfiguracaoBarbearia. A configuracao tem ID global e unicidade por empresa;
nao ha mais consulta pelo ID fixo 1. Login e dia de funcionamento tambem sao
unicos por empresa.

Bloqueios e jornadas derivam a empresa do barbeiro; movimentos, do caixa;
recebimentos, do contrato; descontos, do plano; comissoes, do agendamento.
As tabelas de associacao e o snapshot de servicos herdam o escopo do pai.
Nao ha entidade de produto neste backend.

Todas as 17 interfaces de repositorio consultam entidades com filtro Hibernate
`empresa`, habilitado automaticamente, com `applyToLoadByKey=true`. O parametro
vem exclusivamente de `TenantContext.requireEmpresaId()`. Isso cobre consultas
JPQL, consultas derivadas, paginacao, contagens, agregacoes e buscas por ID.
Filhos usam subconsulta ao pai no filtro, sem repetir `empresa_id`. Operacoes sem
contexto falham; nao existe fallback para empresa padrao ou escopo global.
Caches de segundo nivel e de consultas permanecem desabilitados. Um contexto nao
deve ser trocado dentro de uma transacao/EntityManager em andamento.

Callbacks validam propriedade e referencias nas escritas. Triggers PostgreSQL
validam tambem tabelas de ligacao e impedem transferir registros entre empresas.
Consultas SQL nativas/JDBC nao recebem filtros Hibernate: sao restritas ao cadastro
central e ao provisionamento transacional, com `empresa_id` explicito nos inserts.
Novas consultas de negocio devem usar os repositorios filtrados. Novas entidades
precisam declarar o filtro e a validacao de propriedade, com testes de isolamento.
Referencia: [filtros Hibernate](https://docs.hibernate.org/orm/7.4/javadocs/org/hibernate/annotations/FilterDef.html).

O agendador enumera empresas ativas, abre uma transacao por empresa pelo proxy do
service, registra falhas individualmente e limpa o contexto em `finally`.

## Migrations e dados existentes

Nao alteramos a V1 ja aplicada. A V2 cria o cadastro central quando necessario,
reaproveita `platform_empresa` existente, remove `db_name`, `db_user` e
`db_password`, cria os vinculos, indices e validacoes de integridade.
O antigo `db/platform/V1` e historico e nao e mais executado; todas as novas
migrations ficam em `db/migration`, no unico historico Flyway.

**Bancos antigos nao sao consolidados automaticamente.** Faca backup do cadastro,
dos bancos de cada empresa e das credenciais antes da atualizacao. Preserve a
chave de criptografia antiga enquanto precisar ler backups da arquitetura anterior.
A V2 aborta transacionalmente se encontrar registros operacionais sem empresa.
Nao os atribui silenciosamente a nenhuma empresa.

Para atualizar um banco legado que contem dados de UMA empresa:

1. Aplique a V1, se ainda nao houver um historico Flyway valido, seguindo o guia
   de migrations; nao habilite baseline automatico em banco desconhecido.
2. Antes de executar a V2, garanta que `platform_empresa` exista com a estrutura
   antiga (`db/platform/V1`) e cadastre explicitamente a empresa de destino, com
   UUID, nome, responsavel, slug e estado, preservando o UUID do cadastro central.
   As credenciais antigas obrigatorias podem ser preenchidas com marcadores
   temporarios: a V2 as remove na mesma transacao.
3. Configure `LEGACY_EMPRESA_ID` com esse UUID e inicie a aplicacao para aplicar a
   V2. Todos os dados desse banco devem pertencer exclusivamente a essa empresa.
4. Remova `LEGACY_EMPRESA_ID` da configuracao apos migrar e valide as contagens,
   relacionamentos e acesso pelo subdominio.

Para consolidar varios bancos, use um destino vazio migrado e uma ETL revisada que
remapeie IDs e todas as FKs e tabelas de ligacao. IDs numericos podem colidir entre
bancos antigos. Preserve os UUIDs das empresas. Valide contagens e totais por
empresa antes de substituir a instalacao. Nao exclua os bancos antigos ate conferir
a importacao e o backup. Esse procedimento depende dos bancos existentes e nao e
executado pela aplicacao.

## Dominio e operacao

Configure `APP_PLATFORM_HOST=painel.seusistema.com` e
`APP_TENANT_BASE_DOMAIN=seusistema.com`, DNS wildcard, HTTPS e proxy que preserve
Host e rejeite dominios externos. `X-Forwarded-Host` e `Forwarded` nao selecionam
empresa. Cookies de sessao devem continuar restritos ao host. A plataforma nao
acessa rotas operacionais e empresas nao acessam rotas da plataforma. Suspensao
bloqueia inclusive sessoes ja autenticadas, sem excluir dados.

Use credenciais PostgreSQL sem permissao para criar databases ou roles. O usuario
que aplica migrations precisa das permissoes DDL no banco unico. O backup passa a
ser do banco unico completo; nao ha chave AES nova para credenciais por empresa.

## Testes

```powershell
.\mvnw.cmd test
```

`MultiTenancyIntegrationTest` usa duas empresas no mesmo H2 e testa login,
configuracao, CRUD por IDs externos, parametros forjados, agregacoes, todos os
repositorios de negocio, filhos, sessao, hosts e suspensao. Os testes antigos
possuem contexto explicito criado exclusivamente no classpath de teste.

Os testes PostgreSQL de migracao exigem banco vazio e descartavel:

```powershell
$env:MIGRATION_TEST_DB_URL='jdbc:postgresql://localhost:5432/barber_migration_test'
$env:MIGRATION_TEST_DB_USERNAME='usuario_de_teste'
$env:MIGRATION_TEST_DB_PASSWORD='senha_de_teste'
.\mvnw.cmd '-Dtest=FlywayMigrationIntegrationTest' test
```

Para executar os mesmos testes HTTP e de repositorios contra PostgreSQL, configure
`PLATFORM_TEST_DB_URL`, `PLATFORM_TEST_DB_USERNAME` e `PLATFORM_TEST_DB_PASSWORD`
para outro banco vazio e exclusivo. A classe `MultiTenancyIntegrationTest` passa a
usar Flyway e Hibernate `validate`, mantendo todas as verificacoes de isolamento.
`EmpresaMigrationIntegrationTest` usa schemas aleatorios no banco indicado por
`MIGRATION_TEST_DB_URL` para testar rollback de dados legados e triggers PostgreSQL.

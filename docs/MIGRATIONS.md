# Migrations PostgreSQL

O Flyway controla o banco unico a partir de `src/main/resources/db/migration`.
A V1 cria o esquema operacional original; a V2 adiciona o cadastro central,
`empresa_id`, indices, unicidade por empresa e validacoes de relacionamentos.
As migrations sao aplicadas antes de o Hibernate validar as entidades.

## Banco novo

Prepare as credenciais da plataforma e inicie:

```powershell
.\scripts\Initialize-Platform.ps1
docker compose up -d --build
```

O esquema e criado sem dados operacionais. O administrador da plataforma e criado
com as credenciais configuradas. Cada cadastro de empresa cria apenas seu admin,
configuracao e horarios informados, em uma transacao.

```properties
spring.flyway.enabled=true
spring.jpa.hibernate.ddl-auto=validate
```

O historico unico fica em `flyway_schema_history`. `validate` nao altera tabelas.
`clean` permanece desabilitado. Nao use baseline automatico em banco desconhecido.

## Dados existentes

A V2 aborta transacionalmente se houver registros sem empresa. Para dados de uma
unica empresa, cadastre explicitamente o UUID de destino e configure
`LEGACY_EMPRESA_ID`. Bancos antigos separados precisam de ETL com remapeamento de
IDs e FKs. O procedimento completo esta no [guia multiempresa](MULTIEMPRESA.md).
Nao altere a V1 nem o historico ja aplicado. A migration antiga em `db/platform`
permanece como referencia historica e nao e executada pela aplicacao atual.

## Proximas alteracoes

Crie V3 e versoes seguintes, atualizando entidades e migrations em conjunto.
Nao edite migrations ja aplicadas: o Flyway valida seus checksums.

## Testes PostgreSQL

Use apenas banco vazio e descartavel:

```powershell
$env:MIGRATION_TEST_DB_URL='jdbc:postgresql://localhost:5432/barber_migration_test'
$env:MIGRATION_TEST_DB_USERNAME='usuario_de_teste'
$env:MIGRATION_TEST_DB_PASSWORD='senha_de_teste'
.\mvnw.cmd '-Dtest=FlywayMigrationIntegrationTest,EmpresaMigrationIntegrationTest' test
```

Os testes verificam Flyway, Hibernate `validate`, reaplicacao sem perda de dados,
rollback de legado sem empresa, preservacao de UUID e triggers contra vinculos
entre empresas. Eles deixam registros/schemas no banco descartavel e nao executam
`clean`. Sem essas variaveis, os testes PostgreSQL sao ignorados. Os demais testes
usam H2. O perfil `dev` tambem usa H2 e nao aplica Flyway.

# Migrations do PostgreSQL

O Flyway controla o esquema do banco a partir de `src/main/resources/db/migration`.
A primeira migration, `V1__esquema_inicial.sql`, cria o esquema completo atual e seus índices.
Ela foi preparada para **um banco PostgreSQL vazio**, sem reaproveitar o banco antigo.

## Primeira inicialização

Depois de disponibilizar o banco vazio, execute na pasta do projeto:

```powershell
docker compose up -d --build
docker compose logs app
```

O Flyway aplica a migration antes da validação do Hibernate e registra a versão na
tabela `flyway_schema_history`. As migrations criam somente o esquema, sem dados
da barbearia ou usuários. O cadastro de empresa na plataforma grava seus dados,
os horários informados e o administrador. Não é necessário gerar o `target` localmente.

Configuração de PostgreSQL:

```properties
spring.flyway.enabled=true
spring.jpa.hibernate.ddl-auto=validate
```

O modo `validate` verifica se o banco corresponde às entidades; ele não altera tabelas.
Não habilite `baseline-on-migrate` para tentar reaproveitar o esquema antigo.
O Flyway recusa um banco não vazio sem histórico de migrations. A operação `clean`
fica desabilitada para evitar apagamentos acidentais.

## Próximas mudanças

Crie um novo arquivo, por exemplo `V2__adicionar_empresa.sql`, e inclua os comandos
SQL necessários. Não edite uma migration já aplicada: o Flyway verifica seu checksum.
Atualize as entidades junto com a nova migration e valide o resultado em um banco
separado antes de usar na instalação principal.

O isolamento por banco e o cadastro central estão descritos no [guia multiempresa](MULTIEMPRESA.md).

## Teste com PostgreSQL

O teste `FlywayMigrationIntegrationTest` exige um banco vazio e exclusivo para testes:

```powershell
$env:MIGRATION_TEST_DB_URL = 'jdbc:postgresql://localhost:5432/barber_migration_test'
$env:MIGRATION_TEST_DB_USERNAME = 'usuario_do_teste'
$env:MIGRATION_TEST_DB_PASSWORD = 'senha_do_teste'
.\mvnw.cmd '-Dtest=FlywayMigrationIntegrationTest' test
```

O teste inicia a aplicação com Flyway e Hibernate `validate`, verifica que nenhuma
configuração, horário ou usuário foi criado e confirma que reaplicar as migrations preserva um registro salvo explicitamente pelo teste.
Ele deixa os dados nesse banco de teste e não executa `clean`.
Sem essas variáveis, esse teste é ignorado. Os demais testes continuam usando H2;
os perfis `test` e `dev` mantêm o Flyway desativado e seu próprio modo de criação de esquema.

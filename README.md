# Sistema de Barbearia

Aplicação Spring Boot para uma barbearia, com agendamento público, painel administrativo, agenda do barbeiro, controle de caixa e planos mensais.

A documentação técnica fica em [docs/DOCUMENTACAO_TECNICA.md](docs/DOCUMENTACAO_TECNICA.md).

## Pré-requisitos

- Java 21.
- Maven Wrapper do projeto (`mvnw.cmd` no Windows).
- PostgreSQL 17 para uso real, ou o perfil `dev` com H2 para teste local rápido.
- Docker Desktop, caso queira subir o PostgreSQL pelo `docker-compose.yml`.

## Subir para teste local sem PostgreSQL

Use o perfil `dev`. Ele cria um banco H2 local em `./data/barbearia-dev` e dados demonstrativos automaticamente.

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Depois acesse:

```text
http://localhost:8080
```

Credenciais locais:

```text
Administrador
usuario: admin
senha: admin12345

Barbeiro demo
usuario: barbeiro
senha: barbeiro123
```

Esse modo é apenas para desenvolvimento. Para uso real, use PostgreSQL com Flyway.

## Subir com PostgreSQL via Docker

```powershell
docker compose up -d
```

O banco padrão fica em:

```text
host: localhost
porta: 5432
database: barber
usuario: barber
senha: barber_dev
```

Em seguida execute:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/barber"
$env:DB_USERNAME="barber"
$env:DB_PASSWORD="barber_dev"
$env:APP_INITIAL_ADMIN_USERNAME="admin"
$env:APP_INITIAL_ADMIN_PASSWORD="admin12345"
$env:APP_INITIAL_ADMIN_NAME="Administrador"
$env:APP_DEMO_DATA="true"
.\mvnw.cmd spring-boot:run
```

O Flyway cria o esquema automaticamente. O Hibernate está configurado como `validate`, então a aplicação só sobe se as tabelas estiverem coerentes com as entidades.

## Subir com PostgreSQL instalado

Crie o banco e o usuário:

```sql
create database barber;
create user barber with encrypted password 'barber_dev';
grant all privileges on database barber to barber;
```

Depois configure as variáveis de ambiente conforme `.env.example` e execute `.\mvnw.cmd spring-boot:run`.

## Variáveis principais

- `DB_URL`: URL JDBC do PostgreSQL.
- `DB_USERNAME`: usuário do banco.
- `DB_PASSWORD`: senha do banco.
- `APP_INITIAL_ADMIN_USERNAME`: login do primeiro administrador.
- `APP_INITIAL_ADMIN_PASSWORD`: senha do primeiro administrador.
- `APP_INITIAL_ADMIN_NAME`: nome exibido para o administrador.
- `APP_DEMO_DATA`: use `true` para criar dados demonstrativos.

O administrador inicial só é criado se o login ainda não existir. A senha não é redefinida em reinicializações.

## Executar no IntelliJ

Abra o projeto como Maven, configure Java 21 e use a classe principal:

```text
bruninho.Barbeiro.BarbeiroApplication
```

Para teste local, adicione o profile `dev`. Para PostgreSQL, use as mesmas variáveis de ambiente da seção anterior.

## Rotas principais

- `/`: página pública.
- `/agendar`: agendamento público.
- `/planos`: adesão a plano mensal.
- `/login`: autenticação.
- `/admin`: painel administrativo.
- `/admin/servicos`: catálogo de serviços.
- `/admin/barbeiros`: cadastro de barbeiros.
- `/admin/clientes`: cadastro de clientes.
- `/admin/agenda`: agenda administrativa.
- `/admin/caixa`: controle de caixa.
- `/admin/relatorios`: relatórios.
- `/admin/configuracoes`: configurações da barbearia e mensalidades.
- `/barbeiro/agenda`: agenda do barbeiro autenticado.

## Regras principais

- Os horários são calculados em passos de 15 minutos.
- O sistema considera horário de funcionamento, jornada do barbeiro, intervalo, bloqueios, duração do serviço e agendamentos existentes.
- `CANCELADO` e `NAO_COMPARECEU` não bloqueiam disponibilidade.
- O recebimento financeiro exige caixa aberto e atendimento concluído.
- Cada atendimento possui no máximo um recebimento ativo; estornos preservam o lançamento original.
- Pix e cartão não aumentam o saldo físico esperado em dinheiro.
- Planos mensais controlam quantidade de cortes por mês e por semana.

## Testes

```powershell
.\mvnw.cmd test
```

Os testes usam H2 em modo PostgreSQL. As dependências de Testcontainers PostgreSQL estão no projeto para evolução futura dos testes com Docker.

## Banco de dados

As migrations ficam em `src/main/resources/db/migration`:

- `V1__schema_inicial.sql`: cria o esquema principal em português.
- `V2__planos_mensais.sql`: adiciona configurações e adesões de planos mensais.

Principais tabelas:

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

## Backup e restauração

Backup:

```powershell
pg_dump -h localhost -U barber -d barber -Fc -f barber.backup
```

Restauração:

```powershell
createdb -h localhost -U barber barber_restore
pg_restore -h localhost -U barber -d barber_restore barber.backup
```

## Antes de publicar

Configure HTTPS, cookies seguros, senhas fortes, credenciais fora do repositório, backups automáticos, logs sem dados sensíveis e monitoramento do PostgreSQL. Não use as credenciais de desenvolvimento em produção.

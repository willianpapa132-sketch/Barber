# Sistema de Barbearia

Aplicação Spring Boot para agendamento público, administração e consulta de agenda do barbeiro.

Documentacao tecnica das classes, regras de negocio, telas e controllers: [docs/DOCUMENTACAO_TECNICA.md](docs/DOCUMENTACAO_TECNICA.md).

## Pré-requisitos

- Java 21.
- Maven Wrapper do projeto (`mvnw.cmd` no Windows).
- PostgreSQL 17 ou Docker Desktop para subir o banco via Compose.

## Banco com Docker

```powershell
docker compose up -d
```

O banco padrão fica em `localhost:5432`, database `barber`, usuário `barber`, senha `barber_dev`.

## Banco PostgreSQL já instalado

Crie um banco e usuário próprios:

```sql
create database barber;
create user barber with encrypted password 'barber_dev';
grant all privileges on database barber to barber;
```

Depois configure as variáveis conforme `.env.example`.

## Variáveis principais

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `APP_INITIAL_ADMIN_USERNAME`
- `APP_INITIAL_ADMIN_PASSWORD`
- `APP_INITIAL_ADMIN_NAME`
- `APP_DEMO_DATA=true` para criar dados demonstrativos em desenvolvimento.

O administrador inicial só é criado se o usuário ainda não existir; a senha não é redefinida a cada inicialização.

## Executar no terminal

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/barber"
$env:DB_USERNAME="barber"
$env:DB_PASSWORD="barber_dev"
$env:APP_INITIAL_ADMIN_USERNAME="admin"
$env:APP_INITIAL_ADMIN_PASSWORD="admin12345"
.\mvnw.cmd spring-boot:run
```

Flyway cria o esquema automaticamente. O Hibernate está configurado para validar o esquema.

## Executar no IntelliJ

Abra o projeto Maven, use Java 21 e crie uma configuração para `bruninho.Barber.BarberApplication`. Defina as mesmas variáveis de ambiente da seção anterior.

## Rotas principais

- `/` página pública.
- `/agendar` agendamento público sem conta.
- `/login` acesso restrito.
- `/admin` painel administrativo.
- `/admin/servicos`, `/admin/barbeiros`, `/admin/clientes`, `/admin/agenda`, `/admin/caixa`, `/admin/relatorios`, `/admin/configuracoes`.
- `/barbeiro/agenda` agenda do barbeiro autenticado.

## Regras de agenda

Horários são calculados em passos de 15 minutos, com início inclusivo e fim exclusivo. O sistema considera funcionamento, jornada, intervalo, bloqueios, duração real do serviço e agendamentos existentes. `CANCELADO` e `NAO_COMPARECEU` não bloqueiam disponibilidade. A confirmação bloqueia pessimisticamente a linha do barbeiro antes de verificar conflitos e gravar.

## Regras de caixa

Agendar ou concluir atendimento não gera receita. O recebimento exige caixa aberto e atendimento concluído. Cada atendimento tem um recebimento ativo integral; estornos preservam o lançamento original e liberam novo recebimento. Pix e cartão não aumentam o saldo físico esperado em dinheiro.

## Testes

```powershell
.\mvnw.cmd test
```

Os testes locais usam H2 em modo PostgreSQL porque Docker não estava disponível neste ambiente. As dependências de Testcontainers PostgreSQL estão no projeto para evolução dos testes quando Docker estiver ativo.

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

Configure HTTPS, cookies seguros, senhas fortes, credenciais fora do repositório, backups automáticos, logs sem dados sensíveis e monitoramento do PostgreSQL. Não use as credenciais de desenvolvimento.

## Rodar sem Docker para teste manual

Para ver a aplicacao funcionando sem subir PostgreSQL, use o perfil `dev`. Ele usa um banco H2 local em `./data/barber-dev` e cria dados demonstrativos automaticamente.

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Depois acesse:

```text
http://localhost:8080
```

Login administrador:

```text
usuario: admin
senha: admin12345
```

Login barbeiro demo:

```text
usuario: barbeiro
senha: barbeiro123
```

Esse modo e apenas para teste local. Para uso real, use PostgreSQL com Flyway.

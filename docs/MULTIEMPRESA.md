# Plataforma com um banco por empresa

## Preparar o acesso local

Na pasta `Barber`, execute:

```powershell
.\scripts\Initialize-Platform.ps1
docker compose up -d --build
```

Se o Windows bloquear a execucao de scripts, execute apenas essa preparacao com
politica liberada para o processo, sem alterar a configuracao permanente:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\Initialize-Platform.ps1
```

O script gera um `.env` local, ignorado pelo Git, com a chave AES de 32 bytes e
uma senha aleatória para o proprietário. Ele não sobrescreve um `.env` existente.
Se já houver um arquivo, copie as opções `APP_MULTI...`, `APP_PLATFORM...` e
`APP_TENANT...` do exemplo e preencha os valores sem trocar uma chave em uso.

Abra `http://localhost:8080/plataforma`. Use o login e a senha presentes no `.env`.
O login padrão gerado é `dono@plataforma.local`.
O acesso de proprietário é criado apenas na primeira inicialização: alterar a
senha no `.env` posteriormente não substitui uma senha já cadastrada.

Cadastre uma empresa com nome, responsável, telefone, endereço físico, identificador
do endereço de acesso, e-mail e senha inicial do administrador e os sete dias de
funcionamento. Por exemplo, o identificador `carlos` disponibiliza:

```text
http://carlos.localhost:8080/login
```

Navegadores com suporte a subdomínios de `localhost` resolvem o endereço localmente.
Se o ambiente não resolver esse nome, configure a resolução local para `127.0.0.1`.
Cada empresa recebe somente o administrador, os dados da empresa e os horários
informados no formulário. Não são criados serviços, planos, clientes, barbeiros,
agendamentos ou caixa. As jornadas e demais cadastros são feitos
pelo administrador da empresa.

## Como o isolamento funciona

- `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` apontam para o banco central e para a
  credencial de provisionamento da instalação.
- O banco central mantém o proprietário e o cadastro das empresas. O esquema da
  barbearia também é migrado nesse banco para validação inicial das entidades,
  sem criar configurações, horários, dados demonstrativos ou usuários de barbearia.
- Cada cadastro gera um banco `barber_t_<identificador>` e um usuário próprio
  `barber_u_<identificador>`, sem privilégios de superusuário ou criação de bancos.
- A instalação precisa de permissão para criar bancos e usuários. O usuário
  criado pela imagem oficial PostgreSQL do Compose possui esses privilégios.
  Em serviços gerenciados, configure uma credencial de provisionamento compatível.
- As senhas dos bancos ficam criptografadas com AES-GCM no cadastro central e
  não são devolvidas pela API. Guarde a chave junto com os backups da instalação.
- O backend resolve a empresa pelo `Host`, antes da autenticação e das transações.
  Campos de formulário e cabeçalhos como `X-Empresa-Id` ou `X-Forwarded-Host` não
  selecionam o banco.
- A sessão é vinculada à empresa no login. Uma sessão de outra empresa ou do
  proprietário é recusada, mesmo que seja enviada manualmente para outro endereço.
- Não há fallback para o banco central nas operações de barbearia após a
  inicialização. Subdomínios desconhecidos e empresas suspensas são recusados.
- O painel da plataforma possui autenticação central, papel `PLATFORM_ADMIN` e
  proteção CSRF nas alterações. Administradores de empresas não acessam esse painel.
- Suspender uma empresa bloqueia as novas requisições, inclusive de sessões já
  autenticadas; não apaga seus dados. Reativar restaura o acesso.

## Migrations e inicialização

As migrations das barbearias ficam em `db/migration`, com histórico próprio em
cada banco. O cadastro aplica essas migrations antes de ativar a empresa. Os bancos
ativos são atualizados na inicialização da aplicação; as suspensas são atualizadas
quando reativadas e acessadas. Cada pool permite até cinco conexões e nenhuma
conexão ociosa obrigatória.

As migrations centrais ficam em `db/platform`, com histórico
`platform_schema_history`. O baseline de versão zero é usado apenas nesse histórico
central, para coexistir com o esquema de referência já migrado. O histórico das
barbearias continua exigindo banco vazio ou migrations válidas, sem baseline automático.

O modo de empresa única continua disponível com `APP_MULTITENANCY_ENABLED=false`.
Ativar a plataforma não transfere os registros de uma instalação antiga para uma
empresa: esta implementação cria empresas novas, como combinado para o banco novo.

## Domínio real

Exemplo de configuração:

```properties
APP_PLATFORM_HOST=painel.seusistema.com
APP_TENANT_BASE_DOMAIN=seusistema.com
```

Configure o DNS do painel e `*.seusistema.com` para o servidor, com HTTPS e
certificado adequado. O proxy deve preservar o `Host` original e rejeitar nomes
fora dos domínios configurados. A aplicação não interpreta cabeçalhos de
encaminhamento para selecionar empresas. Os cookies de sessão devem permanecer
restritos ao host, sem domínio compartilhado entre as barbearias.

## Backup

Mantenha backup do banco central, de cada banco de empresa e da chave
`APP_TENANT_ENCRYPTION_KEY`. Restaurar somente um banco de barbearia não restaura o
cadastro central nem suas credenciais. Não gere outra chave sobre uma instalação
existente sem um procedimento de troca de chaves.

## Validação em PostgreSQL separado

O teste `MultiTenancyIntegrationTest` exige um PostgreSQL exclusivo, com permissão
para criar bancos e usuários. Ele cria duas empresas e testa autenticação,
separação de dados, suspensão, CSRF e bloqueio de conexão ao banco de outra empresa.
Use um contêiner descartável, pois ele cria bancos e usuários de teste.

```powershell
$env:PLATFORM_TEST_DB_URL='jdbc:postgresql://localhost:5432/platform_test'
$env:PLATFORM_TEST_DB_USERNAME='usuario_do_teste'
$env:PLATFORM_TEST_DB_PASSWORD='senha_do_teste'
.\mvnw.cmd '-Dtest=MultiTenancyIntegrationTest' test
```

Sem essas variáveis, esse teste é ignorado e os demais continuam no modo de empresa única.

Os dados usados pelos testes automatizados ficam exclusivamente em `src/test` e
no perfil `test`. O inicializador de exemplos não é distribuído no JAR da aplicação.

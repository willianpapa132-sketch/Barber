# Documentacao Tecnica do Sistema

Este documento explica o que cada classe representa, onde ficam as regras de negocio, quais telas existem e quais funcionalidades cada controller entrega.

## Visao Geral

O sistema foi organizado em camadas:

- `domain`: entidades JPA e enums que representam os dados principais do sistema.
- `repository`: interfaces Spring Data JPA para consultar e gravar dados.
- `service`: regras de negocio e operacoes transacionais.
- `web`: controllers MVC que recebem requisicoes, montam telas e chamam servicos.
- `web.form`: DTOs de formulario usados para evitar mass assignment.
- `config`: configuracoes da aplicacao, seguranca e inicializacao.
- `templates`: telas Thymeleaf.

O banco e criado pelo Flyway em `src/main/resources/db/migration/V1__schema_inicial.sql`. O JPA esta configurado para validar o esquema, nao para cria-lo em producao.

## Classe Principal

### `BarberApplication`

Classe de entrada da aplicacao Spring Boot.

Responsabilidades:

- Inicializar o contexto Spring.
- Carregar controllers, services, repositories, entidades e configuracoes.

Nao possui regra de negocio.

## Configuracao

### `AppProperties`

Representa configuracoes lidas de `application.properties` e variaveis de ambiente.

Campos principais:

- `app.zone`: fuso horario usado pelo sistema.
- `app.demo-data`: liga ou desliga dados demonstrativos.
- `app.initial-admin.*`: dados para criar o primeiro administrador.

### `SecurityConfig`

Configura a seguranca da aplicacao.

Responsabilidades:

- Habilitar login por sessao.
- Configurar logout seguro.
- Habilitar CSRF nos formularios.
- Definir permissoes por rota.
- Criar o `PasswordEncoder` com BCrypt.
- Carregar usuarios ativos pelo banco.

Regras de acesso:

- `/`, `/agendar/**`, `/css/**`, `/js/**`, `/login`: publico.
- `/admin/**`: somente `ADMIN`.
- `/barbeiro/**`: somente `BARBEIRO`.
- Demais rotas: exigem autenticacao.

### `DataInitializer`

Executa ao iniciar a aplicacao.

Responsabilidades:

- Criar o administrador inicial, se as variaveis forem informadas e o usuario ainda nao existir.
- Criar dados demonstrativos quando `APP_DEMO_DATA=true`.

Importante: a senha do administrador inicial nao e redefinida se o usuario ja existir.

## Entidades e Enums de Dominio

### `AppUser`

Representa um usuario que pode fazer login.

Campos importantes:

- `username`
- `passwordHash`
- `nome`
- `role`
- `active`

Pode ser administrador ou barbeiro. Usuarios inativos nao conseguem autenticar.

### `Role`

Enum com os perfis do sistema:

- `ADMIN`
- `BARBEIRO`

### `ServiceCatalog`

Representa um servico oferecido pela barbearia.

Exemplos:

- Corte masculino.
- Barba.
- Corte + barba.

Campos principais:

- `nome`
- `descricao`
- `preco`
- `duracaoMinutos`
- `active`

Regra importante: servicos nao sao excluidos fisicamente no fluxo normal. Eles podem ser desativados para preservar historico.

### `Barber`

Representa um barbeiro/profissional.

Campos principais:

- `nome`
- `telefone`
- `active`
- `user`
- `services`
- `version`

O campo `version` permite controle de concorrencia otimista no JPA. No agendamento, a linha do barbeiro tambem e bloqueada pessimisticamente para evitar duas reservas no mesmo horario.

### `BarberWorkSchedule`

Representa a jornada semanal de um barbeiro.

Campos principais:

- `barber`
- `dayOfWeek`
- `startTime`
- `endTime`
- `breakStart`
- `breakEnd`
- `active`

Usada para calcular horarios disponiveis.

### `BarberBlock`

Representa bloqueios, folgas ou indisponibilidades em uma data especifica.

Campos principais:

- `barber`
- `blockDate`
- `startTime`
- `endTime`
- `reason`

Se `startTime` e `endTime` forem nulos, o bloqueio pode representar a data inteira.

### `Customer`

Representa um cliente cadastrado pelo administrador.

Campos principais:

- `nome`
- `telefone`
- `telefoneNormalizado`

Regra importante: agendamento publico nao altera automaticamente um cliente existente por telefone. Isso evita sobrescrever cadastro interno com dados digitados no fluxo publico.

### `Appointment`

Representa um atendimento/agendamento.

Campos principais:

- `confirmationCode`
- `customer`
- `barber`
- `service`
- `customerName`
- `customerPhone`
- `serviceNameSnapshot`
- `servicePriceSnapshot`
- `serviceDurationMinutesSnapshot`
- `startAt`
- `endAt`
- `status`
- `paymentReceived`

Regra importante: o agendamento salva copia do nome, preco e duracao do servico no momento da reserva. Alteracoes futuras no catalogo nao alteram historico.

### `AppointmentStatus`

Enum com os status do atendimento:

- `AGENDADO`
- `CONCLUIDO`
- `CANCELADO`
- `NAO_COMPARECEU`

Status de pagamento fica separado em `paymentReceived`.

### `CashSession`

Representa uma abertura de caixa.

Campos principais:

- `openedAt`
- `closedAt`
- `openedByUser`
- `closedByUser`
- `initialCash`
- `expectedCash`
- `countedCash`
- `differenceCash`
- `status`

Regra importante: so pode existir uma sessao de caixa aberta por vez. O banco possui indice unico parcial para garantir isso.

### `CashSessionStatus`

Enum com status do caixa:

- `ABERTO`
- `FECHADO`

### `CashMovement`

Representa uma movimentacao financeira.

Campos principais:

- `cashSession`
- `appointment`
- `originalMovement`
- `type`
- `paymentMethod`
- `amount`
- `description`
- `category`
- `createdByUser`
- `reversalReason`
- `reversed`

Regra importante: movimentacoes nao sao apagadas. Correcao e feita por estorno.

### `CashMovementType`

Enum com tipos de movimento:

- `RECEBIMENTO_SERVICO`
- `ENTRADA_MANUAL`
- `SAIDA_MANUAL`
- `SUPRIMENTO`
- `SANGRIA`
- `ESTORNO`

### `PaymentMethod`

Enum com formas informativas de recebimento:

- `DINHEIRO`
- `PIX`
- `CARTAO_DEBITO`
- `CARTAO_CREDITO`

Pix e cartao nao aumentam o saldo fisico esperado em dinheiro.

### `BarberShopConfig`

Representa configuracoes gerais da barbearia.

Campos principais:

- `nome`
- `telefone`
- `endereco`
- `minAntecedenciaMinutos`
- `horizonteDias`

### `ShopHours`

Representa horario geral de funcionamento da barbearia por dia da semana.

Campos principais:

- `dayOfWeek`
- `openTime`
- `closeTime`
- `closed`

## Repositories

Repositories sao interfaces Spring Data JPA. Eles nao devem conter regra de negocio complexa; sua funcao e consultar e persistir dados.

### `AppUserRepository`

Consulta usuarios do sistema.

Metodos principais:

- `findByUsername`
- `existsByUsername`

Usado por seguranca, inicializacao e cadastro de barbeiro.

### `ServiceCatalogRepository`

Consulta servicos.

Metodos principais:

- `findByActiveTrueOrderByNome`
- `findAllByOrderByNome`

### `BarberRepository`

Consulta barbeiros.

Metodos principais:

- `findByActiveTrueOrderByNome`
- `findByUserUsername`
- `findActiveByService`
- `lockById`

`lockById` aplica bloqueio pessimista na linha do barbeiro durante agendamento.

### `BarberWorkScheduleRepository`

Consulta jornadas dos barbeiros.

Metodos principais:

- `findByBarberIdOrderByDayOfWeek`
- `deleteByBarberId`

### `BarberBlockRepository`

Consulta bloqueios de barbeiros.

Metodos principais:

- `findByBarberIdAndBlockDate`
- `findByBarberIdOrderByBlockDateDescStartTimeDesc`

### `CustomerRepository`

Consulta clientes.

Metodo principal:

- `findTop30ByNomeContainingIgnoreCaseOrTelefoneNormalizadoContainingOrderByNome`

Usado na pesquisa administrativa por nome ou telefone.

### `AppointmentRepository`

Consulta agendamentos.

Metodos principais:

- `findConflicts`: procura sobreposicoes para disponibilidade.
- `findAgenda`: agenda de um barbeiro no periodo.
- `search`: filtro administrativo.
- `findByCustomerIdOrderByStartAtDesc`
- `countByStatusAndStartAtBetween`

### `CashSessionRepository`

Consulta sessoes de caixa.

Metodos principais:

- `findByStatus`
- `lockOpenSession`

`lockOpenSession` bloqueia a sessao aberta para registrar movimentos de forma consistente.

### `CashMovementRepository`

Consulta movimentacoes financeiras.

Metodos principais:

- `findByAppointmentIdAndTypeAndReversedFalse`
- `findByCreatedAtBetweenOrderByCreatedAtDesc`
- `findByCashSessionIdOrderByCreatedAt`

### `BarberShopConfigRepository`

Consulta a configuracao geral da barbearia.

### `ShopHoursRepository`

Consulta horario geral de funcionamento.

Metodo principal:

- `findByDayOfWeek`

## Services e Regras de Negocio

### `BusinessException`

Excecao de negocio usada para mensagens claras ao usuario.

Exemplos:

- Horario indisponivel.
- Caixa fechado.
- Recebimento duplicado.
- Telefone invalido.

### `PhoneNormalizer`

Normaliza e valida telefone.

Regras:

- Remove caracteres nao numericos.
- Exige telefone brasileiro com DDD.
- Aceita 10 ou 11 digitos.

### `CurrentUserService`

Obtem o usuario autenticado no banco.

Usado em operacoes que precisam registrar autor, como caixa e acoes administrativas.

### `AvailabilityService`

Calcula horarios disponiveis e valida se um horario pode ser reservado.

Regras aplicadas:

- Servico precisa existir e estar ativo.
- Barbeiro precisa existir e estar ativo.
- Barbeiro precisa executar o servico escolhido.
- Data nao pode ultrapassar horizonte configurado.
- Horario deve respeitar passos de 15 minutos.
- Horario deve respeitar funcionamento da loja.
- Horario deve respeitar jornada do barbeiro.
- Atendimento nao pode atravessar intervalo.
- Atendimento nao pode bater com bloqueio ou folga.
- Atendimento nao pode sobrepor outro `AGENDADO` ou `CONCLUIDO`.
- `CANCELADO` e `NAO_COMPARECEU` nao bloqueiam agenda.

Observacao: a verificacao usa intervalos com inicio inclusivo e fim exclusivo. Um atendimento que termina as 10h permite outro iniciar as 10h.

### `AppointmentService`

Cria e altera agendamentos.

Metodos principais:

- `createPublic`: cria agendamento vindo da area publica.
- `createManual`: cria agendamento pelo administrador.
- `reschedule`: remarca agendamento.
- `changeStatus`: muda status do atendimento.

Regras importantes:

- Revalida disponibilidade no servidor.
- Bloqueia a linha do barbeiro antes de confirmar.
- Salva snapshot de nome, preco e duracao do servico.
- Gera codigo aleatorio nao sequencial.
- Nao altera cliente cadastrado automaticamente no agendamento publico.
- Permite transicoes de status apenas a partir de `AGENDADO` para `CONCLUIDO`, `CANCELADO` ou `NAO_COMPARECEU`.

### `AdminCatalogService`

Centraliza cadastros administrativos.

Metodos principais:

- `saveService`: cria ou atualiza servico.
- `saveBarber`: cria barbeiro, usuario de acesso e associacao com servicos.
- `defaultSchedule`: cria jornada padrao para barbeiro.
- `saveCustomer`: cria ou atualiza cliente.

Regras importantes:

- Senha de barbeiro e gravada com hash seguro.
- Telefone de barbeiro e cliente e validado.
- Servico tem preco e duracao validados pelo formulario e pelo banco.

### `CashService`

Controla as regras de caixa.

Metodos principais:

- `open`: abre caixa.
- `receipt`: registra recebimento de atendimento concluido.
- `manual`: registra entrada, saida, suprimento ou sangria.
- `reverse`: estorna movimento.
- `close`: fecha caixa.
- `expectedCash`: calcula saldo fisico esperado.

Regras importantes:

- So existe um caixa aberto por vez.
- Caixa fechado nao aceita movimento.
- Atendimento precisa estar `CONCLUIDO` para ser recebido.
- Agendar ou concluir nao gera receita automaticamente.
- Recebimento duplicado e bloqueado por regra de servico e indice unico no banco.
- Estorno nao apaga nem altera o lancamento original; cria outro movimento referenciando o original.
- Depois do estorno, o atendimento pode receber novo pagamento.
- Dinheiro aumenta saldo fisico.
- Pix e cartao entram em receita recebida, mas nao aumentam dinheiro na gaveta.
- Suprimento aumenta dinheiro fisico, mas nao e receita de servico.
- Sangria reduz dinheiro fisico, mas nao e despesa de servico.

## Forms / DTOs de Tela

### `PublicAppointmentForm`

Formulario do agendamento publico.

Campos:

- `serviceId`
- `barberId`
- `date`
- `time`
- `customerName`
- `customerPhone`

### `AppointmentForm`

Formulario de agendamento manual pelo administrador.

Campos:

- `customerId`
- `customerName`
- `customerPhone`
- `serviceId`
- `barberId`
- `date`
- `time`

### `ServiceForm`

Formulario de servico.

Campos:

- `nome`
- `descricao`
- `preco`
- `duracaoMinutos`
- `active`

### `BarberForm`

Formulario de barbeiro.

Campos:

- `nome`
- `telefone`
- `username`
- `password`
- `active`
- `serviceIds`

### `CustomerForm`

Formulario de cliente.

Campos:

- `nome`
- `telefone`

### `CashForms`

Agrupa formularios do caixa:

- `OpenCashForm`: abertura de caixa.
- `ReceiptForm`: recebimento de atendimento.
- `ManualMovementForm`: movimento manual.
- `CloseCashForm`: fechamento.
- `ReversalForm`: estorno.

## Controllers e Funcionalidades

### `RootController`

Rotas:

- `GET /`
- `GET /login`
- `GET /pos-login`

Funcionalidades:

- Exibe pagina inicial publica.
- Exibe tela de login.
- Redireciona usuario autenticado para `/admin` ou `/barbeiro/agenda` conforme perfil.

Telas usadas:

- `public/home.html`
- `login.html`

### `PublicBookingController`

Base: `/agendar`

Rotas:

- `GET /agendar`
- `POST /agendar`
- `GET /agendar/confirmado`

Funcionalidades:

- Lista servicos ativos.
- Lista barbeiros ativos que executam o servico escolhido.
- Mostra horarios disponiveis para servico, barbeiro e data.
- Recebe nome e telefone do cliente.
- Confirma agendamento publico.
- Mostra codigo aleatorio de confirmacao.

Telas usadas:

- `public/agendar.html`
- `public/confirmado.html`

Regra importante: a tela publica nao mostra dados de outros clientes e nao permite consultar agendamento por telefone.

### `AdminController`

Base: `/admin`

Rotas principais:

- `GET /admin`
- `GET /admin/servicos`
- `POST /admin/servicos`
- `POST /admin/servicos/{id}`
- `GET /admin/barbeiros`
- `POST /admin/barbeiros`
- `GET /admin/clientes`
- `POST /admin/clientes`
- `GET /admin/agenda`
- `POST /admin/agenda`
- `POST /admin/agenda/{id}/status`

Funcionalidades:

- Dashboard administrativo.
- Cadastro e ativacao/desativacao de servicos.
- Cadastro de barbeiros e usuario de acesso.
- Associacao de barbeiro com servicos.
- Jornada padrao ao cadastrar barbeiro.
- Cadastro e pesquisa de clientes.
- Visualizacao de agenda.
- Criacao manual de agendamento.
- Conclusao, cancelamento e falta.

Telas usadas:

- `admin/dashboard.html`
- `admin/servicos.html`
- `admin/barbeiros.html`
- `admin/clientes.html`
- `admin/agenda.html`

### `CashController`

Base: `/admin/caixa`

Rotas:

- `GET /admin/caixa`
- `POST /admin/caixa/abrir`
- `POST /admin/caixa/receber`
- `POST /admin/caixa/movimento`
- `POST /admin/caixa/estornar/{id}`
- `POST /admin/caixa/fechar`

Funcionalidades:

- Abrir caixa.
- Listar movimentos da sessao aberta.
- Registrar recebimento de atendimento concluido.
- Registrar entrada manual.
- Registrar saida manual.
- Registrar suprimento.
- Registrar sangria.
- Estornar lancamento.
- Fechar caixa com dinheiro contado.

Tela usada:

- `admin/caixa.html`

Somente administradores acessam.

### `AdminSettingsController`

Base: `/admin`

Rotas:

- `GET /admin/configuracoes`
- `POST /admin/configuracoes`
- `GET /admin/relatorios`

Funcionalidades:

- Configurar nome, telefone, endereco, antecedencia minima e horizonte de agendamento.
- Exibir relatorios financeiros e operacionais por periodo.

Telas usadas:

- `admin/configuracoes.html`
- `admin/relatorios.html`

Observacao: os relatorios financeiros usam a data da movimentacao financeira. Os relatorios de atendimento usam a data do atendimento.

### `BarberAreaController`

Base: `/barbeiro`

Rotas:

- `GET /barbeiro/agenda`

Funcionalidades:

- Mostra apenas a agenda do barbeiro autenticado.
- Permite filtrar por data.
- Exibe cliente, servico, inicio, fim e status.

Tela usada:

- `barbeiro/agenda.html`

Regra importante: o controller busca o barbeiro pelo usuario da sessao (`findByUserUsername`). Ele nao aceita ID de barbeiro pela URL, impedindo acesso a agenda de outro profissional por troca de parametro.

## Telas Thymeleaf

### Area Publica

#### `public/home.html`

Pagina inicial da barbearia.

Mostra:

- Nome da barbearia.
- Chamada para agendamento.
- Aviso de pagamento presencial.
- Link para area restrita.

#### `public/agendar.html`

Tela principal do fluxo publico.

Campos:

- Servico.
- Barbeiro.
- Data.
- Horario.
- Nome.
- Telefone.

Comportamento:

- Ao escolher servico, barbeiro ou data, a tela recarrega para recalcular barbeiros e horarios disponiveis.
- Ao confirmar, envia POST para `/agendar`.

#### `public/confirmado.html`

Tela de confirmacao.

Mostra:

- Codigo aleatorio do agendamento.
- Aviso de pagamento presencial.
- Instrucao para entrar em contato com a barbearia em caso de cancelamento/remarcacao.

### Login

#### `login.html`

Tela de autenticacao.

Campos:

- Usuario.
- Senha.

Inclui CSRF.

### Area Administrativa

#### `admin/dashboard.html`

Painel inicial do administrador.

Mostra:

- Atendimentos concluidos do dia.
- Cancelamentos.
- Faltas.
- Agendamentos do dia.
- Menu administrativo.

#### `admin/servicos.html`

Cadastro e listagem de servicos.

Funcionalidades:

- Criar servico.
- Listar servicos.
- Ativar/desativar servico.

#### `admin/barbeiros.html`

Cadastro e listagem de barbeiros.

Funcionalidades:

- Cadastrar barbeiro.
- Criar usuario de acesso.
- Definir senha inicial.
- Associar servicos executados.
- Criar jornada padrao.

#### `admin/clientes.html`

Cadastro e pesquisa de clientes.

Funcionalidades:

- Pesquisar por nome ou telefone.
- Cadastrar cliente.

#### `admin/agenda.html`

Gestao de agenda administrativa.

Funcionalidades:

- Filtrar agenda por data e barbeiro.
- Criar agendamento manual.
- Marcar como concluido.
- Cancelar.
- Marcar falta.

#### `admin/caixa.html`

Gestao do caixa.

Funcionalidades:

- Abrir caixa.
- Registrar recebimento.
- Registrar movimento manual.
- Estornar movimento.
- Fechar caixa.
- Ver movimentos da sessao aberta.

#### `admin/configuracoes.html`

Configuracoes da barbearia.

Funcionalidades:

- Editar nome.
- Editar telefone.
- Editar endereco.
- Editar antecedencia minima.
- Editar horizonte de agendamento.

#### `admin/relatorios.html`

Relatorios administrativos.

Mostra:

- Receita recebida liquida de estornos.
- Despesas.
- Atendimentos concluidos.
- Cancelamentos e faltas.
- Movimentacoes financeiras do periodo.

### Area do Barbeiro

#### `barbeiro/agenda.html`

Agenda somente leitura do barbeiro autenticado.

Mostra:

- Data escolhida.
- Inicio.
- Fim.
- Cliente.
- Servico.
- Status.

Nao possui botoes de alteracao.

## Fluxos de Negocio

### Fluxo de Agendamento Publico

1. Cliente acessa `/agendar`.
2. Escolhe servico.
3. Escolhe barbeiro que executa esse servico.
4. Escolhe data.
5. Sistema calcula horarios disponiveis.
6. Cliente escolhe horario e informa nome/telefone.
7. `PublicBookingController` recebe o formulario.
8. `AppointmentService.createPublic` valida telefone e chama regra de criacao.
9. Sistema bloqueia a linha do barbeiro.
10. `AvailabilityService` revalida disponibilidade.
11. Agendamento e salvo com snapshot do servico.
12. Sistema mostra codigo aleatorio de confirmacao.

### Fluxo de Agenda Administrativa

1. Admin acessa `/admin/agenda`.
2. Visualiza ou filtra agenda.
3. Pode criar agendamento manual.
4. Pode mudar status para concluido, cancelado ou falta.
5. As mesmas regras de disponibilidade sao usadas no agendamento manual.

### Fluxo do Barbeiro

1. Barbeiro faz login.
2. Sistema redireciona para `/barbeiro/agenda`.
3. Controller identifica o barbeiro pelo usuario logado.
4. Agenda exibida contem apenas atendimentos daquele barbeiro.
5. Tela nao permite escrita.

### Fluxo de Caixa

1. Admin abre caixa com saldo inicial em dinheiro.
2. Concluir atendimento nao gera receita automaticamente.
3. Admin registra recebimento de atendimento concluido.
4. Sistema cria movimento financeiro.
5. Se o pagamento for dinheiro, aumenta saldo fisico esperado.
6. Se for Pix ou cartao, registra receita, mas nao altera dinheiro fisico.
7. Para corrigir erro, admin estorna o lancamento.
8. Fechamento registra dinheiro contado e diferenca.

## Protecoes Importantes

- CSRF habilitado nos formularios.
- Senhas com BCrypt.
- Usuarios inativos nao autenticam.
- Barbeiro nao acessa `/admin/**`.
- Barbeiro nao informa ID para consultar agenda de outro profissional.
- Preco e duracao do servico nao sao confiados ao navegador.
- Recebimento duplicado e bloqueado no servico e no banco.
- Disponibilidade e revalidada no servidor.
- Agendamento usa bloqueio pessimista do barbeiro para evitar corrida.

## Limitacoes Atuais da Implementacao

- A tela de configuracao edita dados gerais, antecedencia e horizonte; horarios gerais e jornadas detalhadas ainda nao possuem editor completo na interface.
- A tela de barbeiro e somente leitura, como definido no escopo.
- Testes locais rodam com H2 em modo PostgreSQL quando Docker nao esta disponivel.
- Testcontainers PostgreSQL esta configurado como dependencia, mas precisa de Docker ativo para ser usado em testes futuros.

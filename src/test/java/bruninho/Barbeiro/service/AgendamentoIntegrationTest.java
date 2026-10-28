package bruninho.Barbeiro.service;

import bruninho.Barbeiro.Controller.cliente.home.agendamentos.CriarAgendamentoRequest;
import bruninho.Barbeiro.domain.*;
import bruninho.Barbeiro.exception.BusinessException;
import bruninho.Barbeiro.exception.NotFoundException;
import bruninho.Barbeiro.exception.HorarioIndisponivelException;
import bruninho.Barbeiro.repository.*;
import bruninho.Barbeiro.security.model.*;
import bruninho.Barbeiro.security.repository.UsuarioRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import static org.hamcrest.Matchers.containsString;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:agendamento_test;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH")
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AgendamentoIntegrationTest {
    @Autowired AgendamentoService service;
    @Autowired DisponibilidadeAgendamentosService disponibilidade;
    @Autowired AgendamentoRepository agendamentos;
    @Autowired UsuarioRepository usuarios;
    @Autowired ClienteRepository clientes;
    @Autowired BarbeiroRepository barbeiros;
    @Autowired ServicoRepository servicos;
    @Autowired JornadaBarbeiroRepository jornadas;
    @Autowired HorarioFuncionamentoRepository horarios;
    @Autowired BloqueioBarbeiroRepository bloqueios;
    @Autowired ConfiguracaoBarbeariaRepository configuracoes;
    @Autowired PlanoMensalRepository planosMensais;
    @Autowired PlanoMensalClienteRepository planosClientes;
    @Autowired MockMvc mvc;
    @Autowired FinalizacaoAgendamentoService finalizacao;
    @Autowired CaixaService caixa;
    @Autowired ComissaoBarbeiroRepository comissoes;
    @Autowired ComissaoPlanoRepository comissoesPlano;
    @Autowired jakarta.persistence.EntityManager entityManager;

    Usuario usuario;
    Cliente cliente;
    Barbeiro barbeiro;
    Servico corte;
    Servico barba;
    JornadaBarbeiro jornada;
    LocalDate dia;

    @BeforeEach
    void preparar() {
        agendamentos.deleteAll();
        planosClientes.deleteAll();
        planosMensais.deleteAll();
        bloqueios.deleteAll();
        jornadas.deleteAll();
        clientes.deleteAll();
        barbeiros.deleteAll();
        servicos.deleteAll();
        usuarios.deleteAll();
        usuarios.flush();
        var config = configuracoes.findById(1L).orElseThrow();
        config.setDiasMaximoAntecedentia(10);
        configuracoes.save(config);
        usuario = novoUsuario("cliente", ROLE.CLIENTE);
        cliente = new Cliente();
        cliente.setUsuario(usuario);
        cliente.setNome("Cliente Teste");
        cliente.setTelefone("11999990000");
        cliente = clientes.save(cliente);
        barbeiro = new Barbeiro();
        barbeiro.setNome("Barbeiro");
        barbeiro.setTelefone("11999991111");
        barbeiro.setUsuario(novoUsuario("barbeiro", ROLE.BARBEIRO));
        barbeiro = barbeiros.save(barbeiro);
        corte = novoServico("Corte", 30, "40.00");
        barba = novoServico("Barba", 15, "20.00");
        dia = LocalDate.now().plusDays(1);
        var horario = horarios.findByDiaSemana(dia.getDayOfWeek());
        horario.setFechado(false);
        horario.setHoraAbertura(LocalTime.of(9, 0));
        horario.setHoraFechamento(LocalTime.of(18, 0));
        horario.setIntervaloMinimoEntreAgendamentos(15);
        horario.setPermiteAgendamentoPlano(true);
        horarios.save(horario);
        jornada = new JornadaBarbeiro();
        jornada.setBarbeiro(barbeiro);
        jornada.setDiaSemana(dia.getDayOfWeek());
        jornada.setHoraInicio(LocalTime.of(8, 0));
        jornada.setHoraFim(LocalTime.of(19, 0));
        jornada.setIntervaloInicio(LocalTime.NOON);
        jornada.setIntervaloFim(LocalTime.of(13, 0));
        jornada.setAtivo(true);
        jornada.setFolga(false);
        jornada = jornadas.save(jornada);
    }

    @Test
    void criaESalvaComValoresDoCadastro() {
        var salvo = service.agendar(request(LocalTime.of(10, 0)));
        assertThat(salvo.getId()).isNotNull();
        assertThat(agendamentos.count()).isEqualTo(1);
        assertThat(salvo.getPrecoTotal()).isEqualByComparingTo("60.00");
        assertThat(salvo.getDuracaoServicoMinutos()).isEqualTo(45);
        assertThat(salvo.getHoraFinalizacao()).isEqualTo(LocalTime.of(10, 45));
        assertThat(salvo.getNomeCliente()).isEqualTo(cliente.getNome());
        assertThat(salvo.getTelefoneCliente()).isEqualTo(cliente.getTelefone());
        assertThat(salvo.getStatus()).isEqualTo(StatusAgendamento.AGENDADO);
        assertThat(salvo.getPagamentoRecebido()).isFalse();
        assertThat(salvo.getPlanoMensalCliente()).isNull();
        assertThat(salvo.getCriadoPorUsuario()).isNull();
        assertThat(salvo.getCriadoEm()).isNotNull();
        corte.setPreco(new BigDecimal("99.00"));
        servicos.save(corte);
        assertThat(agendamentos.findById(salvo.getId()).orElseThrow().getPrecoTotal()).isEqualByComparingTo("60.00");
    }

    @Test
    void clienteComPlanoUsaFluxoDePlanoAutomaticamente() {
        var planoCliente = contratarPlano();

        var salvo = service.agendar(request(LocalTime.of(10, 0)));

        assertThat(salvo.getPlanoMensalCliente().getId()).isEqualTo(planoCliente.getId());
        assertThat(salvo.getPrecoTotal()).isEqualByComparingTo("0.00");
        assertThat(salvo.getHoraFinalizacao()).isEqualTo(LocalTime.of(10, 45));
    }

    @Test
    void clienteComPlanoPodeEscolherQualquerBarbeiroNoAgendamento() {
        var planoCliente = contratarPlano();
        var outroBarbeiro = new Barbeiro();
        outroBarbeiro.setNome("Outro Barbeiro");
        outroBarbeiro.setTelefone("11999992222");
        outroBarbeiro.setUsuario(novoUsuario("outro-barbeiro", ROLE.BARBEIRO));
        outroBarbeiro = barbeiros.save(outroBarbeiro);

        jornada.setBarbeiro(outroBarbeiro);
        jornadas.save(jornada);

        var request = new CriarAgendamentoRequest(usuario.getId(), cliente.getId(),
                outroBarbeiro.getId(), ids(), dia, LocalTime.of(10, 0));

        var salvo = service.agendarComPlano(request);

        assertThat(salvo.getPlanoMensalCliente().getId()).isEqualTo(planoCliente.getId());
        assertThat(salvo.getBarbeiro().getId()).isEqualTo(outroBarbeiro.getId());
    }

    @Test
    void telaDeAgendamentoComPlanoListaTodosOsServicos() throws Exception {
        novoServico("Pigmentacao", 40, "70.00");
        contratarPlano();

        var resposta = mvc.perform(get("/api/cliente/home/agendar").with(user("cliente").roles("CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clienteId").value(cliente.getId()))
                .andExpect(jsonPath("$.usuarioId").value(usuario.getId()))
                .andExpect(jsonPath("$.possuiPlano").value(true))
                .andExpect(jsonPath("$.barbeiros[0].id").value(barbeiro.getId()))
                .andExpect(jsonPath("$.servicos.length()").value(3))
                .andExpect(jsonPath("$.servicos[0].inclusoNoPlano").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(resposta).contains("Corte", "Barba");
        assertThat(resposta).contains("Pigmentacao");
    }

    @Test
    void planoPermiteApenasUmAgendamentoNaoCanceladoPorSemana() {
        contratarPlano();
        var primeiro = service.agendar(request(LocalTime.of(10, 0)));

        assertThatThrownBy(() -> service.agendar(request(LocalTime.of(11, 0))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("apenas um agendamento por semana");

        primeiro.setStatus(StatusAgendamento.CANCELADO);
        agendamentos.save(primeiro);

        var novo = service.agendar(request(LocalTime.of(10, 0)));
        assertThat(novo.getId()).isNotNull();
    }

    @Test
    void planoNaoAgendaEmDiaNaoPermitidoParaPlano() {
        contratarPlano();
        var horario = horarios.findByDiaSemana(dia.getDayOfWeek());
        horario.setPermiteAgendamentoPlano(false);
        horarios.save(horario);

        assertThatThrownBy(() -> service.agendar(request(LocalTime.of(10, 0))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("nao permite agendamento neste dia");
    }

    @Test
    void respeitaFuncionamentoAlmocoDuracaoEEncaixeExato() {
        assertThat(horas()).contains(LocalTime.of(9, 0), LocalTime.of(11, 15), LocalTime.of(13, 0), LocalTime.of(17, 15))
                .doesNotContain(LocalTime.of(8, 0), LocalTime.of(11, 30), LocalTime.NOON, LocalTime.of(17, 30));
        assertThat(disponibilidade.diasDisponiveis(usuario.getId(), cliente.getId(), barbeiro.getId(), ids())).contains(dia);
        service.agendar(request(LocalTime.of(10, 0)));
        assertThat(horas()).contains(LocalTime.of(9, 15), LocalTime.of(10, 45))
                .doesNotContain(LocalTime.of(9, 30), LocalTime.of(10, 0), LocalTime.of(10, 30));
        assertThatThrownBy(() -> service.agendar(request(LocalTime.of(10, 15))))
                .isInstanceOf(HorarioIndisponivelException.class).hasMessage("Horário indisponível");
        assertThat(agendamentos.count()).isEqualTo(1);
    }

    @Test
    void canceladoLiberaHorarioEDiaLotadoNaoAparece() {
        var ocupado = service.agendar(request(LocalTime.of(9, 0)));
        ocupado.setHoraInicio(LocalTime.of(9, 0));
        ocupado.setHoraFinalizacao(LocalTime.of(18, 0));
        agendamentos.save(ocupado);
        assertThat(horas()).isEmpty();
        assertThat(disponibilidade.diasDisponiveis(usuario.getId(), cliente.getId(), barbeiro.getId(), ids())).doesNotContain(dia);
        ocupado.setStatus(StatusAgendamento.CANCELADO);
        agendamentos.save(ocupado);
        assertThat(horas()).contains(LocalTime.of(9, 0));
    }

    @Test
    void fechamentoFolgaAusenciaDeJornadaEBloqueioRetornamVazio() {
        var horario = horarios.findByDiaSemana(dia.getDayOfWeek());
        horario.setFechado(true);
        horarios.save(horario);
        assertThat(horas()).isEmpty();
        horario.setFechado(false);
        horarios.save(horario);
        jornada.setFolga(true);
        jornadas.save(jornada);
        assertThat(horas()).isEmpty();
        jornada.setFolga(false);
        jornadas.save(jornada);
        var bloqueio = new BloqueioBarbeiro();
        bloqueio.setBarbeiro(barbeiro);
        bloqueio.setDataBloqueio(dia);
        bloqueios.save(bloqueio);
        assertThat(horas()).isEmpty();
        bloqueios.deleteAll();
        jornadas.deleteAll();
        assertThat(horas()).isEmpty();
    }

    @Test
    void rejeitaVinculoIncorretoERegistrosInativos() {
        var outro = novoUsuario("outro", ROLE.CLIENTE);
        assertThatThrownBy(() -> service.agendar(new CriarAgendamentoRequest(outro.getId(), cliente.getId(),
                barbeiro.getId(), ids(), dia, LocalTime.of(10, 0))))
                .isInstanceOf(BusinessException.class).hasMessageContaining("não pertence");
        usuario.setAtivo(false);
        usuarios.save(usuario);
        assertThatThrownBy(() -> service.agendar(request(LocalTime.of(10, 0)))).hasMessageContaining("cliente ativo");
        usuario.setAtivo(true);
        usuario.setRole(ROLE.ADMIN);
        usuarios.save(usuario);
        assertThatThrownBy(() -> service.agendar(request(LocalTime.of(10, 0)))).hasMessageContaining("cliente ativo");
        usuario.setRole(ROLE.CLIENTE);
        usuarios.save(usuario);
        barbeiro.setAtivo(false);
        barbeiros.save(barbeiro);
        assertThatThrownBy(() -> service.agendar(request(LocalTime.of(10, 0)))).hasMessageContaining("Barbeiro inativo");
        barbeiro.setAtivo(true);
        barbeiros.save(barbeiro);
        corte.setAtivo(false);
        servicos.save(corte);
        assertThatThrownBy(() -> service.agendar(request(LocalTime.of(10, 0)))).hasMessageContaining("Serviço inativo");
        assertThat(agendamentos.count()).isZero();
    }

    @Test
    void rejeitaServicosDuplicadosInexistentesEDataPassada() {
        assertThatThrownBy(() -> service.agendar(new CriarAgendamentoRequest(usuario.getId(), cliente.getId(),
                barbeiro.getId(), List.of(corte.getId(), corte.getId()), dia, LocalTime.of(10, 0))))
                .hasMessageContaining("sem repetições");
        assertThatThrownBy(() -> service.agendar(new CriarAgendamentoRequest(usuario.getId(), cliente.getId(),
                barbeiro.getId(), List.of(Long.MAX_VALUE), dia, LocalTime.of(10, 0))))
                .isInstanceOf(NotFoundException.class).hasMessageContaining("Serviço não encontrado");
        assertThatThrownBy(() -> service.agendar(new CriarAgendamentoRequest(usuario.getId(), cliente.getId(),
                barbeiro.getId(), ids(), LocalDate.now().minusDays(1), LocalTime.of(10, 0))))
                .hasMessageContaining("futuro");
        assertThat(agendamentos.count()).isZero();
    }

    @Test
    void antecedenciaEForaDaGrade() {
        assertThat(disponibilidade.horariosDiaDisponivel(usuario.getId(), cliente.getId(), barbeiro.getId(),
                LocalDate.now().plusDays(11), ids())).isEmpty();
        assertThatThrownBy(() -> service.agendar(request(LocalTime.of(10, 1)))).hasMessageContaining("indisponível");
        var config = configuracoes.findById(1L).orElseThrow();
        config.setDiasMaximoAntecedentia(0);
        configuracoes.save(config);
        assertThat(horas()).isEmpty();
    }

    @Test
    void configuracaoInvalidaNaoCausaLoop() {
        var horario = horarios.findByDiaSemana(dia.getDayOfWeek());
        horario.setIntervaloMinimoEntreAgendamentos(null);
        horarios.save(horario);
        assertThatThrownBy(this::horas).hasMessageContaining("inválidos");
    }

    @Test
    void criacoesSimultaneasReservamUmaUnicaVez() throws Exception {
        var inicio = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Integer> criar = () -> {
                inicio.await(5, TimeUnit.SECONDS);
                try {
                    service.agendar(request(LocalTime.of(10, 0)));
                    return 201;
                } catch (HorarioIndisponivelException e) {
                    return 409;
                }
            };
            var primeira = executor.submit(criar);
            var segunda = executor.submit(criar);
            inicio.countDown();
            assertThat(List.of(primeira.get(15, TimeUnit.SECONDS), segunda.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
            assertThat(agendamentos.count()).isEqualTo(1);
        }
    }

    @Test
    void endpointCriaSemExporEntidadesETraduzErros() throws Exception {
        String json = """
                {"usuarioId":%d,"clienteId":%d,"barbeiroId":%d,"servicosIds":[%d,%d],
                 "data":"%s","horaInicio":"10:00"}
                """.formatted(usuario.getId(), cliente.getId(), barbeiro.getId(), corte.getId(), barba.getId(), dia);
        mvc.perform(post("/agendamentos").with(user("teste")).with(csrf())
                        .contentType("application/json").content(json))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.precoTotal").value(60.0))
                .andExpect(jsonPath("$.duracaoServicoMinutos").value(45))
                .andExpect(jsonPath("$.status").value("AGENDADO"))
                .andExpect(jsonPath("$.usuario").doesNotExist())
                .andExpect(jsonPath("$.cliente").doesNotExist())
                .andExpect(jsonPath("$.senha").doesNotExist());
        mvc.perform(post("/agendamentos").with(user("teste")).with(csrf())
                        .contentType("application/json").content(json))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.mensagem").value("Horário indisponível"));
        mvc.perform(post("/agendamentos").with(user("teste")).with(csrf())
                        .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensagem").isString());
        mvc.perform(get("/agendamentos/disponibilidade/dias").with(user("teste"))
                        .param("usuarioId", Long.toString(Long.MAX_VALUE)).param("clienteId", cliente.getId().toString())
                        .param("barbeiroId", barbeiro.getId().toString()).param("servicosIds", corte.getId().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void homeDoClienteMostraSomenteProximoAgendamentoFuturoEAgendado() throws Exception {
        novoAgendamento(LocalDate.now().minusDays(1), LocalTime.of(9, 0), StatusAgendamento.AGENDADO);
        novoAgendamento(LocalDate.now().plusDays(1), LocalTime.of(9, 0), StatusAgendamento.CANCELADO);
        novoAgendamento(LocalDate.now().plusDays(1), LocalTime.of(10, 0), StatusAgendamento.CONCLUIDO);
        novoAgendamento(LocalDate.now().plusDays(1), LocalTime.of(11, 0), StatusAgendamento.NAO_COMPARECEU);
        var agendado = novoAgendamento(LocalDate.now().plusDays(1), LocalTime.of(12, 0), StatusAgendamento.AGENDADO);

        mvc.perform(get("/api/cliente/home").with(user("cliente").roles("CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cliente.nome").value("Cliente Teste"))
                .andExpect(jsonPath("$.proximoAtendimento.id").value(agendado.getId()))
                .andExpect(jsonPath("$.proximoAtendimento.dayName").isString())
                .andExpect(jsonPath("$.proximoAtendimento.dayNumber").value(String.valueOf(agendado.getData().getDayOfMonth())))
                .andExpect(jsonPath("$.proximoAtendimento.startsAt").value("12:00"))
                .andExpect(jsonPath("$.proximoAtendimento.endsAt").value("12:30"))
                .andExpect(jsonPath("$.proximoAtendimento.barberName").value("Barbeiro"))
                .andExpect(jsonPath("$.proximoAtendimento.services[0]").value("Corte"))
                .andExpect(jsonPath("$.proximoAtendimento.precoTotal").value(40.0))
                .andExpect(jsonPath("$.proximoAtendimento.status").doesNotExist())
                .andExpect(jsonPath("$.proximoAtendimento.details").doesNotExist())
                .andExpect(jsonPath("$.proximoAtendimento.data").doesNotExist())
                .andExpect(jsonPath("$.latestAppointments").doesNotExist());
    }

    @Test
    void detalheDoAgendamentoDoClienteRetornaPayloadCompleto() throws Exception {
        var agendado = novoAgendamento(LocalDate.now().plusDays(1), LocalTime.of(12, 0), StatusAgendamento.AGENDADO);

        mvc.perform(get("/api/cliente/home/agendamentos/{id}", agendado.getId()).with(user("cliente").roles("CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(agendado.getId()))
                .andExpect(jsonPath("$.data").value(agendado.getData().toString()))
                .andExpect(jsonPath("$.startsAt").value("12:00"))
                .andExpect(jsonPath("$.endsAt").value("12:30"))
                .andExpect(jsonPath("$.services[0]").value("Corte"))
                .andExpect(jsonPath("$.details").value("30 minutos"))
                .andExpect(jsonPath("$.status").value("AGENDADO"))
                .andExpect(jsonPath("$.precoTotal").value(40.0));
    }

    @Test
    void homeDoClienteRetornaProximoAtendimentoNuloQuandoNaoExisteAgendadoFuturo() throws Exception {
        novoAgendamento(LocalDate.now().plusDays(1), LocalTime.of(9, 0), StatusAgendamento.CANCELADO);
        novoAgendamento(LocalDate.now().plusDays(1), LocalTime.of(10, 0), StatusAgendamento.CONCLUIDO);
        novoAgendamento(LocalDate.now().plusDays(1), LocalTime.of(11, 0), StatusAgendamento.NAO_COMPARECEU);

        mvc.perform(get("/api/cliente/home").with(user("cliente").roles("CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cliente.nome").value("Cliente Teste"))
                .andExpect(jsonPath("$.proximoAtendimento").doesNotExist());
    }

    @Test
    void endpointsDisponibilidadeRetornamDatasEHoras() throws Exception {
        mvc.perform(get("/agendamentos/disponibilidade/dias").with(user("teste"))
                        .param("usuarioId", usuario.getId().toString()).param("clienteId", cliente.getId().toString())
                        .param("barbeiroId", barbeiro.getId().toString()).param("servicosIds", corte.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0]").value(dia.toString()));
        mvc.perform(get("/agendamentos/disponibilidade/horarios").with(user("teste"))
                        .param("usuarioId", usuario.getId().toString()).param("clienteId", cliente.getId().toString())
                        .param("barbeiroId", barbeiro.getId().toString()).param("servicosIds", corte.getId().toString())
                        .param("data", dia.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0]").value("09:00:00"));
    }

    @Test
    void clienteComAgendamentoAvulsoContrataPlanoEAgendaPeloEndpointComum() throws Exception {
        var avulso = service.agendar(request(LocalTime.of(10, 0)));
        var planoCliente = contratarPlano();
        // Reproduz registros antigos: o inicializador Java nao substitui NULL lido do banco.
        planoCliente.setPagamentoAtrasado(null);
        planosClientes.saveAndFlush(planoCliente);

        mvc.perform(post("/api/cliente/home/agendar").with(user("cliente").roles("CLIENTE"))
                        .contentType("application/json")
                        .content("""
                                {"usuarioId":%d,"clienteId":%d,"barbeiroId":%d,
                                 "servicosIds":[%d],"data":"%s","horaInicio":"11:00"}
                                """.formatted(usuario.getId(), cliente.getId(), barbeiro.getId(), corte.getId(), dia)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.precoTotal").value(0));

        var registros = agendamentos.findAll();
        assertThat(registros).hasSize(2);
        var mensal = registros.stream().filter(a -> !a.getId().equals(avulso.getId())).findFirst().orElseThrow();
        assertThat(mensal.getPlanoMensalCliente().getId()).isEqualTo(planoCliente.getId());
        assertThat(agendamentos.findById(avulso.getId()).orElseThrow().getPlanoMensalCliente()).isNull();
    }

    @Test
    void pagamentoAtrasadoContinuaBloqueandoAgendamentoMensalCom400() throws Exception {
        var planoCliente = contratarPlano();
        planoCliente.setPagamentoAtrasado(true);
        planosClientes.saveAndFlush(planoCliente);

        mvc.perform(post("/api/cliente/home/agendar").with(user("cliente").roles("CLIENTE"))
                        .contentType("application/json")
                        .content("""
                                {"usuarioId":%d,"clienteId":%d,"barbeiroId":%d,
                                 "servicosIds":[%d],"data":"%s","horaInicio":"11:00"}
                                """.formatted(usuario.getId(), cliente.getId(), barbeiro.getId(), corte.getId(), dia)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("plano esta com pagamento atrasado"));
        assertThat(agendamentos.count()).isZero();
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void planoMistoPreservaCondicoesECobraExtrasComComissaoEPontosSeparados() throws Exception {
        caixa.abrir(new bruninho.Barbeiro.Controller.Admin.caixa.AbrirCaixaRequest(BigDecimal.ZERO));
        var contrato = contratarPlano();
        var plano = planosMensais.findById(contrato.getPlanoMensal().getId()).orElseThrow();
        var pigmentacao = novoServico("Pigmentacao", 20, "50.00");
        var bigode = novoServico("Bigode extra", 10, "10.00");
        var regra = new ServicosPlanosDesconto();
        regra.setPlanoMensal(plano); regra.setPorcentagemDesconto(20);
        regra.setServicos(new HashSet<>(List.of(pigmentacao)));
        plano.getDescontosServicos().add(regra);
        planosMensais.saveAndFlush(plano);
        barbeiro.setPorcentagemComissao(40); barbeiros.saveAndFlush(barbeiro);

        mvc.perform(get("/api/cliente/home/agendar").with(user("cliente").roles("CLIENTE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.servicos.length()").value(4))
                .andExpect(jsonPath("$.servicos[3].porcentagemDesconto").value(20))
                .andExpect(jsonPath("$.servicos[3].precoLiquido").value(40));
        var salvo = service.agendar(new CriarAgendamentoRequest(usuario.getId(), cliente.getId(),
                barbeiro.getId(), List.of(corte.getId(), pigmentacao.getId(), bigode.getId()), dia, LocalTime.of(10, 0)));
        assertThat(salvo.getDuracaoServicoMinutos()).isEqualTo(60);
        assertThat(salvo.getPrecoTotal()).isEqualByComparingTo("50.00");
        assertThat(CalculoServicosPlano.pontos(salvo.getServicosPlano())).isEqualTo(30);

        pigmentacao.setPreco(new BigDecimal("100.00")); servicos.save(pigmentacao);
        corte.setDuracaoMinutos(90); servicos.save(corte);
        regra.setPorcentagemDesconto(50);
        salvo.setData(LocalDate.now().minusDays(1)); agendamentos.saveAndFlush(salvo);
        entityManager.clear();
        var previa = finalizacao.consultar("barbeiro", salvo.getId(), null);
        assertThat(previa.valorCobrar()).isEqualByComparingTo("50.00");
        assertThat(previa.comissaoPlano().pontos()).isEqualTo(30);
        assertThat(previa.formasPagamentoDisponiveis()).doesNotContain(FormaPagamento.MENSAL);
        assertThatThrownBy(() -> finalizacao.finalizar("barbeiro", salvo.getId(), FormaPagamento.MENSAL))
                .hasMessageContaining("pagamento dos servicos extras");
        var resposta = finalizacao.finalizar("barbeiro", salvo.getId(), FormaPagamento.PIX);
        assertThat(resposta.comissao().valorBase()).isEqualByComparingTo("50.00");
        assertThat(resposta.comissao().valorComissao()).isEqualByComparingTo("20.00");
        assertThat(resposta.comissaoPlano().pontos()).isEqualTo(30);
        assertThat(comissoesPlano.findByAgendamento_Id(salvo.getId()).orElseThrow().getPontos()).isEqualTo(30);
        assertThat(comissoes.existsByAgendamento_Id(salvo.getId())).isTrue();
        assertThatThrownBy(() -> finalizacao.finalizar("barbeiro", salvo.getId(), FormaPagamento.PIX))
                .hasMessageContaining("AGENDADO");
    }

    private List<Long> ids() { return List.of(corte.getId(), barba.getId()); }

    private CriarAgendamentoRequest request(LocalTime hora) {
        return new CriarAgendamentoRequest(usuario.getId(), cliente.getId(), barbeiro.getId(), ids(), dia, hora);
    }

    private List<LocalTime> horas() {
        return disponibilidade.horariosDiaDisponivel(usuario.getId(), cliente.getId(), barbeiro.getId(), dia, ids());
    }

    private Usuario novoUsuario(String login, ROLE role) {
        var u = new Usuario();
        u.setLogin(login);
        u.setSenha("hash-teste");
        u.setAtivo(true);
        u.setRole(role);
        return usuarios.save(u);
    }

    private Servico novoServico(String nome, int minutos, String preco) {
        var s = new Servico();
        s.setNome(nome);
        s.setDuracaoMinutos(minutos);
        s.setPreco(new BigDecimal(preco));
        return servicos.save(s);
    }

    private Agendamento novoAgendamento(LocalDate data, LocalTime horaInicio, StatusAgendamento status) {
        var agendamento = new Agendamento();
        agendamento.setCliente(cliente);
        agendamento.setBarbeiro(barbeiro);
        agendamento.setServico(new HashSet<>(List.of(corte)));
        agendamento.setData(data);
        agendamento.setHoraInicio(horaInicio);
        agendamento.setHoraFinalizacao(horaInicio.plusMinutes(corte.getDuracaoMinutos()));
        agendamento.setDuracaoServicoMinutos(corte.getDuracaoMinutos());
        agendamento.setPrecoTotal(corte.getPreco());
        agendamento.setNomeCliente(cliente.getNome());
        agendamento.setTelefoneCliente(cliente.getTelefone());
        agendamento.setStatus(status);
        agendamento.setPagamentoRecebido(false);
        agendamento.setCriadoEm(LocalDateTime.now());
        return agendamentos.save(agendamento);
    }

    private PlanoMensalCliente contratarPlano() {
        var plano = new PlanosMensal();
        plano.setNomePlano("Plano Corte e Barba");

        plano.setDescricao("Corte e barba inclusos no plano mensal");
        plano.setValorMensal(new BigDecimal("150.00"));
        plano.setAtivo(true);
        plano.setServicosIncluidos(new HashSet<>(List.of(corte, barba)));
        plano.setDiasMaximoAntecedencia(10);
        plano = planosMensais.save(plano);

        var planoCliente = new PlanoMensalCliente();
        planoCliente.setCliente(cliente);
        planoCliente.setUsuario(usuario);
        planoCliente.setPlanoMensal(plano);
        planoCliente.setValorMensal(plano.getValorMensal());
        planoCliente.setDiasMaximoAntecedencia(plano.getDiasMaximoAntecedencia());
        planoCliente.setAtivo(true);
        return planosClientes.save(planoCliente);
    }
}

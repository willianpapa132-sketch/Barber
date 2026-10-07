package bruninho.Barbeiro.service;

import bruninho.Barbeiro.Controller.cliente.request.CriarAgendamentoRequest;
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

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
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
    @Autowired MockMvc mvc;

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
        bloqueios.deleteAll();
        jornadas.deleteAll();
        clientes.deleteAll();
        barbeiros.deleteAll();
        servicos.deleteAll();
        usuarios.deleteAll();
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
        assertThat(salvo.getPlanoMensal()).isNull();
        assertThat(salvo.getCriadoPorUsuario()).isNull();
        assertThat(salvo.getCriadoEm()).isNotNull();
        corte.setPreco(new BigDecimal("99.00"));
        servicos.save(corte);
        assertThat(agendamentos.findById(salvo.getId()).orElseThrow().getPrecoTotal()).isEqualByComparingTo("60.00");
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
}

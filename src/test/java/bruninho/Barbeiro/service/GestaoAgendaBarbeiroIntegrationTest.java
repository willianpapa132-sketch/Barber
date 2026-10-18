package bruninho.Barbeiro.service;

import bruninho.Barbeiro.domain.*;
import bruninho.Barbeiro.repository.*;
import bruninho.Barbeiro.security.model.*;
import bruninho.Barbeiro.security.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:gestao_agenda_test;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH")
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class GestaoAgendaBarbeiroIntegrationTest {
    private static final String LOGIN = "agenda-gestao@demo.com";
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired BarbeiroRepository barbeiros;
    @Autowired ClienteRepository clientes;
    @Autowired ServicoRepository servicos;
    @Autowired JornadaBarbeiroRepository jornadas;
    @Autowired HorarioFuncionamentoRepository funcionamento;
    @Autowired AgendamentoRepository agendamentos;
    @Autowired BloqueioBarbeiroRepository bloqueios;
    @Autowired PlanoMensalRepository planos;
    @Autowired PlanoMensalClienteRepository contratos;
    @Autowired DisponibilidadeAgendamentosService disponibilidade;
    private Barbeiro barbeiro;
    private Cliente cliente;
    private Servico corte;
    private JornadaBarbeiro jornada;
    private LocalDate dia;

    @BeforeEach
    void preparar() {
        dia = LocalDate.now().plusDays(1);
        barbeiro = new Barbeiro(); barbeiro.setNome("Barbeiro da agenda"); barbeiro.setTelefone("11999990001");
        barbeiro.setUsuario(usuario(LOGIN, ROLE.BARBEIRO)); barbeiro.setPorcentagemComissao(40);
        barbeiro = barbeiros.save(barbeiro);
        cliente = new Cliente(); cliente.setNome("Maria Cliente"); cliente.setTelefone("11999990002");
        cliente.setUsuario(usuario("cliente-gestao@demo.com", ROLE.CLIENTE)); cliente = clientes.save(cliente);
        corte = new Servico(); corte.setNome("Corte"); corte.setPreco(new BigDecimal("45.00")); corte.setDuracaoMinutos(15);
        corte = servicos.save(corte);
        var horario = funcionamento.findByDiaSemana(dia.getDayOfWeek());
        horario.setFechado(false); horario.setHoraAbertura(LocalTime.of(8, 0)); horario.setHoraFechamento(LocalTime.of(19, 0));
        horario.setIntervaloMinimoEntreAgendamentos(15); horario.setPermiteAgendamentoPlano(true); funcionamento.save(horario);
        jornada = new JornadaBarbeiro(); jornada.setBarbeiro(barbeiro); jornada.setDiaSemana(dia.getDayOfWeek());
        jornada.setHoraInicio(LocalTime.of(8, 10)); jornada.setHoraFim(LocalTime.of(18, 10));
        jornada.setIntervaloInicio(LocalTime.of(12, 10)); jornada.setIntervaloFim(LocalTime.of(13, 10));
        jornada.setAtivo(true); jornada.setFolga(false); jornada = jornadas.save(jornada);
    }

    @Test
    void gradeComecaETerminaNaJornadaESegueIntervaloConfigurado() throws Exception {
        mvc.perform(get("/api/barbeiro/agenda/grade").param("data", dia.toString()).with(user(LOGIN).roles("BARBEIRO")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.inicio").value("08:10:00"))
                .andExpect(jsonPath("$.fim").value("18:10:00"))
                .andExpect(jsonPath("$.intervaloGrade").value(15))
                .andExpect(jsonPath("$.horarios[0]").value("08:10:00"))
                .andExpect(jsonPath("$.horarios[1]").value("08:25:00"))
                .andExpect(jsonPath("$.horarios.length()").value(40))
                .andExpect(jsonPath("$.podeCriar").value(true));
        assertThat(horarios()).contains(LocalTime.of(8, 10)).doesNotContain(LocalTime.of(8, 0));
    }

    @Test
    void gradeNaoCortaODiaEmVinteAgendamentosEFiltraStatus() throws Exception {
        for (int i = 0; i < 25; i++) salvar(LocalTime.of(8, 10).plusMinutes(i * 15L), StatusAgendamento.AGENDADO);
        salvar(LocalTime.of(16, 10), StatusAgendamento.CANCELADO);
        mvc.perform(get("/api/barbeiro/agenda/grade").param("data", dia.toString()).with(user(LOGIN).roles("BARBEIRO")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.agendamentos.length()").value(26));
        mvc.perform(get("/api/barbeiro/agenda/grade").param("data", dia.toString()).param("status", "CANCELADO")
                        .with(user(LOGIN).roles("BARBEIRO")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.agendamentos.length()").value(1));
    }

    @Test
    void barbeiroCriaAtendimentoParaClienteComUsuarioCriadorCorreto() throws Exception {
        postJson("/agendamentos", atendimento("09:10", false)).andExpect(status().isCreated());
        var salvo = agendamentos.findAll().getFirst();
        assertThat(salvo.getCliente().getId()).isEqualTo(cliente.getId());
        assertThat(salvo.getBarbeiro().getId()).isEqualTo(barbeiro.getId());
        assertThat(salvo.getCriadoPorUsuario().getId()).isEqualTo(barbeiro.getUsuario().getId());
        assertThat(salvo.getPrecoTotal()).isEqualByComparingTo("45.00");
        assertThat(salvo.getHoraFinalizacao()).isEqualTo(LocalTime.of(9, 25));
    }

    @Test
    void barbeiroCriaAvulsoSemCadastrarUsuarioOuCliente() throws Exception {
        long quantidadeClientes = clientes.count();
        postJson("/agendamentos", atendimento("09:10", true)).andExpect(status().isCreated());
        var salvo = agendamentos.findAll().getFirst();
        assertThat(salvo.getCliente()).isNull(); assertThat(salvo.getNomeCliente()).isEqualTo("Cliente avulso");
        assertThat(clientes.count()).isEqualTo(quantidadeClientes);
    }

    @Test
    void clienteMensalSegueRegrasERecebeVinculoDoPlano() throws Exception {
        var plano = new PlanosMensal(); plano.setNomePlano("Plano da agenda"); plano.setDescricao("Corte");

        plano.setValorMensal(new BigDecimal("100.00")); plano.setAtivo(true);
        plano.setServicosIncluidos(new HashSet<>(List.of(corte))); plano.setDiasMaximoAntecedencia(30); plano = planos.save(plano);
        var contrato = new PlanoMensalCliente(); contrato.setCliente(cliente); contrato.setUsuario(cliente.getUsuario());
        contrato.setPlanoMensal(plano); contrato.setValorMensal(plano.getValorMensal()); contrato.setAtivo(true);
        contrato.setDiasMaximoAntecedencia(30); contrato = contratos.save(contrato);
        var foraDoPlano = new Servico(); foraDoPlano.setNome("Barba"); foraDoPlano.setDuracaoMinutos(10);
        foraDoPlano.setPreco(new BigDecimal("30.00")); foraDoPlano = servicos.save(foraDoPlano);
        mvc.perform(get("/api/barbeiro/agenda/clientes/" + cliente.getId() + "/servicos").with(user(LOGIN).roles("BARBEIRO")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.possuiPlano").value(true))
                .andExpect(jsonPath("$.nomePlano").value("Plano da agenda"))
                .andExpect(jsonPath("$.servicos.length()").value(1))
                .andExpect(jsonPath("$.servicos[0].id").value(corte.getId()));
        postJson("/agendamentos", "{\"clienteId\":%d,\"servicosIds\":[%d],\"data\":\"%s\",\"horaInicio\":\"09:10\"}"
                .formatted(cliente.getId(), foraDoPlano.getId(), dia)).andExpect(status().isBadRequest());
        assertThat(agendamentos.count()).isZero();
        postJson("/agendamentos", atendimento("09:10", false)).andExpect(status().isCreated());
        var salvo = agendamentos.findAll().getFirst();
        assertThat(salvo.getPlanoMensalCliente().getId()).isEqualTo(contrato.getId());
        assertThat(salvo.getPrecoTotal()).isZero();
    }

    @Test
    void clienteSemPlanoRecebeCatalogoAtivoCompleto() throws Exception {
        var barba = new Servico(); barba.setNome("Barba"); barba.setDuracaoMinutos(10);
        barba.setPreco(new BigDecimal("30.00")); servicos.save(barba);
        mvc.perform(get("/api/barbeiro/agenda/clientes/" + cliente.getId() + "/servicos").with(user(LOGIN).roles("BARBEIRO")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.possuiPlano").value(false))
                .andExpect(jsonPath("$.servicos.length()").value(2));
        mvc.perform(get("/api/barbeiro/agenda/servicos").with(user(LOGIN).roles("BARBEIRO")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void consultaDeServicosRejeitaClienteInexistenteEPerfilCliente() throws Exception {
        mvc.perform(get("/api/barbeiro/agenda/clientes/999999/servicos").with(user(LOGIN).roles("BARBEIRO")))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/barbeiro/agenda/clientes/" + cliente.getId() + "/servicos").with(user("cliente").roles("CLIENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void bloqueioParcialNaoFechaODiaENaoPermiteAgendamentoSobreposto() throws Exception {
        postJson("/bloqueios", bloqueio("09:10", "09:40")).andExpect(status().isCreated());
        assertThat(horarios()).contains(LocalTime.of(8, 10), LocalTime.of(9, 40))
                .doesNotContain(LocalTime.of(9, 10), LocalTime.of(9, 25));
        postJson("/agendamentos", atendimento("09:25", false)).andExpect(status().isConflict());
        postJson("/agendamentos", atendimento("09:25", true)).andExpect(status().isConflict());
        postJson("/agendamentos", atendimento("09:40", true)).andExpect(status().isCreated());
    }

    @Test
    void rejeitaSobreposicaoComAtendimentoIntervaloEBloqueio() throws Exception {
        salvar(LocalTime.of(9, 10), StatusAgendamento.AGENDADO);
        postJson("/bloqueios", bloqueio("09:10", "09:40")).andExpect(status().isConflict());
        postJson("/bloqueios", bloqueio("12:10", "12:40")).andExpect(status().isConflict());
        postJson("/bloqueios", bloqueio("10:10", "10:40")).andExpect(status().isCreated());
        postJson("/bloqueios", bloqueio("10:25", "10:55")).andExpect(status().isConflict());
    }

    @Test
    void validaJornadaGradeEOrdemDosHorarios() throws Exception {
        postJson("/bloqueios", bloqueio("09:00", "09:40")).andExpect(status().isBadRequest());
        postJson("/bloqueios", bloqueio("09:40", "09:10")).andExpect(status().isBadRequest());
        postJson("/bloqueios", bloqueio("18:10", "19:10")).andExpect(status().isBadRequest());
        jornada.setFolga(true); jornadas.saveAndFlush(jornada);
        mvc.perform(get("/api/barbeiro/agenda/grade").param("data", dia.toString()).with(user(LOGIN).roles("BARBEIRO")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.podeCriar").value(false));
        postJson("/bloqueios", bloqueio("09:10", "09:40")).andExpect(status().isBadRequest());
    }

    @Test
    void removeSomenteBloqueioDoProprioBarbeiroELiberaPeriodo() throws Exception {
        postJson("/bloqueios", bloqueio("09:10", "09:40")).andExpect(status().isCreated());
        Long id = bloqueios.findAll().getFirst().getId();
        var outro = new Barbeiro(); outro.setNome("Outro"); outro.setTelefone("11999990003");
        outro.setUsuario(usuario("outro-gestao@demo.com", ROLE.BARBEIRO)); barbeiros.saveAndFlush(outro);
        mvc.perform(delete("/api/barbeiro/agenda/bloqueios/" + id).with(user("outro-gestao@demo.com").roles("BARBEIRO")))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/barbeiro/agenda/bloqueios/" + id).with(user(LOGIN).roles("BARBEIRO")))
                .andExpect(status().isNoContent());
        assertThat(horarios()).contains(LocalTime.of(9, 10));
    }

    @Test
    void clienteNaoPodeUsarEndpointsDeGestao() throws Exception {
        mvc.perform(get("/api/barbeiro/agenda/grade").with(user("cliente").roles("CLIENTE"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/barbeiro/agenda/bloqueios").contentType("application/json").content(bloqueio("09:10", "09:40"))
                .with(user("cliente").roles("CLIENTE"))).andExpect(status().isForbidden());
    }

    @Test
    void pesquisaClienteEServicosSemExporEntidades() throws Exception {
        mvc.perform(get("/api/barbeiro/agenda/clientes").param("busca", "Maria").with(user(LOGIN).roles("BARBEIRO")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].nome").value("Maria Cliente"))
                .andExpect(jsonPath("$[0].usuario").doesNotExist());
        mvc.perform(get("/api/barbeiro/agenda/servicos").with(user(LOGIN).roles("BARBEIRO")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].duracaoMinutos").value(15));
    }

    private ResultActions postJson(String path, String json) throws Exception {
        return mvc.perform(post("/api/barbeiro/agenda" + path).with(user(LOGIN).roles("BARBEIRO"))
                .contentType("application/json").content(json));
    }
    private String atendimento(String hora, boolean avulso) {
        String pessoa = avulso ? "\"nomeCliente\":\"Cliente avulso\",\"telefoneCliente\":\"11999990004\""
                : "\"clienteId\":" + cliente.getId();
        return "{%s,\"servicosIds\":[%d],\"data\":\"%s\",\"horaInicio\":\"%s\"}".formatted(pessoa, corte.getId(), dia, hora);
    }
    private String bloqueio(String inicio, String fim) {
        return "{\"data\":\"%s\",\"horaInicio\":\"%s\",\"horaFim\":\"%s\"}".formatted(dia, inicio, fim);
    }
    private List<LocalTime> horarios() {
        return disponibilidade.horariosDiaDisponivel(cliente.getUsuario().getId(), cliente.getId(), barbeiro.getId(), dia, List.of(corte.getId()));
    }
    private Usuario usuario(String login, ROLE role) {
        var usuario = new Usuario(); usuario.setLogin(login); usuario.setSenha("senha-teste"); usuario.setAtivo(true); usuario.setRole(role);
        return usuarios.save(usuario);
    }
    private void salvar(LocalTime hora, StatusAgendamento status) {
        var item = new Agendamento(); item.setBarbeiro(barbeiro); item.setCliente(cliente); item.setNomeCliente(cliente.getNome());
        item.setServico(new HashSet<>(List.of(corte))); item.setData(dia); item.setHoraInicio(hora); item.setHoraFinalizacao(hora.plusMinutes(15));
        item.setDuracaoServicoMinutos(15); item.setPrecoTotal(corte.getPreco()); item.setStatus(status); agendamentos.save(item);
    }
}

package bruninho.Barbeiro.service;

import bruninho.Barbeiro.Controller.Admin.configuracao.*;
import bruninho.Barbeiro.domain.*;
import bruninho.Barbeiro.repository.*;
import bruninho.Barbeiro.security.model.*;
import bruninho.Barbeiro.security.repository.UsuarioRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:configuracao_test;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH")
@ActiveProfiles("test") @AutoConfigureMockMvc @Transactional
class ConfiguracaoIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired HorariosService service;
    @Autowired HorarioFuncionamentoRepository horarios;
    @Autowired ConfiguracaoBarbeariaRepository configuracoes;
    @Autowired BarbeiroRepository barbeiros;
    @Autowired JornadaBarbeiroRepository jornadas;
    @Autowired AgendamentoRepository agendamentos;
    @Autowired UsuarioRepository usuarios;
    @Autowired ClienteRepository clientes;
    @Autowired PlanoMensalRepository planos;
    @Autowired PlanoMensalClienteRepository contratos;
    @Autowired DisponibilidadeAgendamentosService disponibilidade;
    @Autowired ServicoRepository servicos;
    @Autowired @Qualifier("bootstrap") CommandLineRunner bootstrap;
    private LocalDate dia;
    private Barbeiro barbeiro;
    private Barbeiro outro;
    private PlanoMensalCliente contrato;

    @BeforeEach void preparar() {
        dia=LocalDate.now().plusDays(1);
        barbeiro=barbeiro("config-barbeiro"); outro=barbeiro("config-outro");
        for(var h:horarios.findAll()) { h.setFechado(false); h.setHoraAbertura(LocalTime.of(9,0)); h.setHoraFechamento(LocalTime.of(18,0)); h.setPermiteAgendamentoPlano(true); }
        horarios.flush();
        var c=new Cliente(); c.setNome("Cliente"); c.setTelefone("11999990000"); c.setUsuario(usuario("config-cliente",ROLE.CLIENTE)); c=clientes.save(c);
        var p=new PlanosMensal(); p.setNomePlano("Plano"); p.setDescricao("Plano teste"); p.setValorMensal(new BigDecimal("100")); p.setAtivo(true); p.setDiasMaximoAntecedencia(30); p=planos.save(p);
        contrato=new PlanoMensalCliente(); contrato.setCliente(c); contrato.setUsuario(c.getUsuario()); contrato.setPlanoMensal(p);
        contrato.setAtivo(true); contrato.setValorMensal(p.getValorMensal()); contrato.setDiasMaximoAntecedencia(30);
        contrato.setLiberadoAte(LocalDate.now().plusMonths(1)); contrato.setProximoVencimento(LocalDate.now().plusMonths(1)); contrato=contratos.save(contrato);
    }
    @Test void fechaDiaDaFolgaACadaBarbeiroECancelaPlanosEAvulsosSemApagarHistorico() throws Exception {
        var plano=agendamento(barbeiro,dia,10,StatusAgendamento.AGENDADO); plano.setPlanoMensalCliente(contrato); agendamentos.save(plano);
        var avulso=agendamento(outro,dia.plusWeeks(1),11,StatusAgendamento.AGENDADO);
        var concluido=agendamento(barbeiro,dia,12,StatusAgendamento.CONCLUIDO);
        var passado=agendamento(barbeiro,dia.minusWeeks(1),10,StatusAgendamento.AGENDADO);
        var diferente=agendamento(barbeiro,dia.plusDays(1),10,StatusAgendamento.AGENDADO);
        var r=service.salvar(request(true,"09:00","18:00",true));
        assertThat(r.jornadasAjustadas()).isEqualTo(2); assertThat(r.agendamentosCancelados()).isEqualTo(2);
        assertThat(jornadas.findByDiaSemanaAndBarbeiro_id(dia.getDayOfWeek(),barbeiro.getId()).orElseThrow().getFolga()).isTrue();
        assertThat(plano.getStatus()).isEqualTo(StatusAgendamento.CANCELADO); assertThat(avulso.getStatus()).isEqualTo(StatusAgendamento.CANCELADO);
        assertThat(plano.getMotivoCancelamento()).contains("Configuracao"); assertThat(plano.getAtualizadoEm()).isNotNull();
        assertThat(concluido.getStatus()).isEqualTo(StatusAgendamento.CONCLUIDO);
        assertThat(passado.getStatus()).isEqualTo(StatusAgendamento.AGENDADO); assertThat(diferente.getStatus()).isEqualTo(StatusAgendamento.AGENDADO);
        mvc.perform(get("/api/barbeiro/agenda/grade").param("data",dia.toString()).with(user("config-barbeiro").roles("BARBEIRO")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.podeCriar").value(false));
        assertThat(service.salvar(request(true,"09:00","18:00",true)).agendamentosCancelados()).isZero();
        assertThat(horarios.count()).isEqualTo(7);
    }
    @Test void reduzFuncionamentoAjustaJornadaECancelaSomenteForaDoHorario() {
        var fora=agendamento(barbeiro,dia,9,StatusAgendamento.AGENDADO);
        var dentro=agendamento(barbeiro,dia,11,StatusAgendamento.AGENDADO);
        var r=service.salvar(request(false,"10:00","16:00",true));
        assertThat(r.agendamentosCancelados()).isEqualTo(1); assertThat(fora.getStatus()).isEqualTo(StatusAgendamento.CANCELADO);
        assertThat(dentro.getStatus()).isEqualTo(StatusAgendamento.AGENDADO);
        var j=jornadas.findByDiaSemanaAndBarbeiro_id(dia.getDayOfWeek(),barbeiro.getId()).orElseThrow();
        assertThat(j.getHoraInicio()).isEqualTo(LocalTime.of(10,0)); assertThat(j.getHoraFim()).isEqualTo(LocalTime.of(16,0));
    }
    @Test void desabilitarPlanoCancelaSoPlanoEReabrirNaoRemoveFolgas() {
        var plano=agendamento(barbeiro,dia,10,StatusAgendamento.AGENDADO); plano.setPlanoMensalCliente(contrato);
        var avulso=agendamento(outro,dia,11,StatusAgendamento.AGENDADO);
        service.salvar(request(false,"09:00","18:00",false));
        assertThat(plano.getStatus()).isEqualTo(StatusAgendamento.CANCELADO); assertThat(avulso.getStatus()).isEqualTo(StatusAgendamento.AGENDADO);
        service.salvar(request(true,"09:00","18:00",false)); service.salvar(request(false,"09:00","18:00",true));
        assertThat(jornadas.findByDiaSemanaAndBarbeiro_id(dia.getDayOfWeek(),barbeiro.getId()).orElseThrow().getFolga()).isTrue();
    }
    @Test void reinicioPreservaHorariosEConfiguracoesSalvas() throws Exception {
        service.salvar(request(false,"10:00","16:00",false)); bootstrap.run();
        var h=horarios.findByDiaSemana(dia.getDayOfWeek());
        assertThat(h.getHoraAbertura()).isEqualTo(LocalTime.of(10,0)); assertThat(h.getHoraFechamento()).isEqualTo(LocalTime.of(16,0));
        assertThat(h.getPermiteAgendamentoPlano()).isFalse(); assertThat(configuracoes.findById(1L).orElseThrow().getNome()).isEqualTo("Minha Barbearia");
    }
    @Test void antecedenciaGlobalLimitaClientesComPlano() {
        var config=configuracoes.findById(1L).orElseThrow();config.setDiasMaximoAntecedentia(2);
        var s=new Servico();s.setNome("Corte");s.setDuracaoMinutos(30);s.setPreco(new BigDecimal("50"));s=servicos.save(s);
        assertThat(disponibilidade.horariosDisponiveis(contrato.getUsuario(),barbeiro.getId(),LocalDate.now().plusDays(2),Set.of(s))).isEmpty();
        config.setDiasMaximoAntecedentia(0);
        assertThat(disponibilidade.diasDisponiveis(contrato.getUsuario().getId(),contrato.getCliente().getId(),barbeiro.getId(),List.of(s.getId()))).isEmpty();
    }
    @Test void apiValidaEntradaPermissoesEEdicaoDosDados() throws Exception {
        mvc.perform(get("/api/admin/configuracao").with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andExpect(jsonPath("$.horarios.length()").value(7));
        mvc.perform(put("/api/admin/configuracao").with(user("admin").roles("ADMIN")).contentType("application/json").content("{\"nome\":\"\",\"diasMaximoAntecedencia\":31,\"horarios\":[]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/configuracao").with(user("b").roles("BARBEIRO"))).andExpect(status().isForbidden());
        mvc.perform(get("/admin/configuracao").with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andExpect(forwardedUrl("/client/index.html"));
        var r=request(false,"09:00","18:00",true);
        var json=new tools.jackson.databind.json.JsonMapper().writeValueAsString(r);
        mvc.perform(put("/api/admin/configuracao").with(user("admin").roles("ADMIN")).contentType("application/json").content(json))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Minha Barbearia"));
    }
    @Test @Transactional(propagation=Propagation.NOT_SUPPORTED) @DirtiesContext(methodMode=DirtiesContext.MethodMode.AFTER_METHOD)
    void pagamentoImpedeCancelamentoERollbackPreservaTodaConfiguracao() {
        var a=agendamento(barbeiro,dia,10,StatusAgendamento.AGENDADO);a.setPagamentoRecebido(true);agendamentos.saveAndFlush(a);
        var nome=configuracoes.findById(1L).orElseThrow().getNome();
        assertThatThrownBy(()->service.salvar(request(true,"09:00","18:00",false))).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThat(configuracoes.findById(1L).orElseThrow().getNome()).isEqualTo(nome);
        assertThat(horarios.findByDiaSemana(dia.getDayOfWeek()).getFechado()).isFalse();
        assertThat(jornadas.findByDiaSemanaAndBarbeiro_id(dia.getDayOfWeek(),barbeiro.getId()).orElseThrow().getFolga()).isFalse();
        assertThat(agendamentos.findById(a.getId()).orElseThrow().getStatus()).isEqualTo(StatusAgendamento.AGENDADO);
    }
    private SalvarConfiguracaoRequest request(boolean fechado,String inicio,String fim,boolean plano) {
        var lista=Arrays.stream(DayOfWeek.values()).map(d->new HorarioFuncionamentoBarber(d,
                LocalTime.parse(d==dia.getDayOfWeek()?inicio:"09:00"),LocalTime.parse(d==dia.getDayOfWeek()?fim:"18:00"),d==dia.getDayOfWeek()&&fechado,15,d==dia.getDayOfWeek()?plano:true)).toList();
        return new SalvarConfiguracaoRequest("Minha Barbearia","11999990000","Rua Teste, 10",10,lista);
    }
    private Usuario usuario(String login,ROLE role){var u=new Usuario();u.setLogin(login);u.setSenha("hash");u.setAtivo(true);u.setRole(role);return usuarios.save(u);}
    private Barbeiro barbeiro(String login){
        var b=new Barbeiro();b.setNome(login);b.setTelefone("11999990000");b.setUsuario(usuario(login,ROLE.BARBEIRO));b=barbeiros.save(b);
        for(var d:DayOfWeek.values()){var j=new JornadaBarbeiro();j.setBarbeiro(b);j.setDiaSemana(d);j.setAtivo(true);j.setFolga(false);j.setHoraInicio(LocalTime.of(9,0));j.setHoraFim(LocalTime.of(18,0));jornadas.save(j);}return b;
    }
    private Agendamento agendamento(Barbeiro b,LocalDate data,int hora,StatusAgendamento status){
        var a=new Agendamento();a.setBarbeiro(b);a.setData(data);a.setHoraInicio(LocalTime.of(hora,0));a.setHoraFinalizacao(LocalTime.of(hora,30));
        a.setNomeCliente("Cliente");a.setDuracaoServicoMinutos(30);a.setPrecoTotal(new BigDecimal("50"));a.setStatus(status);a.setPagamentoRecebido(false);return agendamentos.save(a);
    }
}

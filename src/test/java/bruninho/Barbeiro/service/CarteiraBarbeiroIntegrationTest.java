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
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:carteira_test;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH")
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class CarteiraBarbeiroIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired BarbeiroRepository barbeiros;
    @Autowired ClienteRepository clientes;
    @Autowired AgendamentoRepository agendamentos;
    @Autowired ComissaoBarbeiroRepository comissoes;
    @Autowired ComissaoPlanoRepository pontos;
    @Autowired ServicoRepository servicos;
    @Autowired PlanoMensalRepository planos;
    @Autowired PlanoMensalClienteRepository contratos;
    private Barbeiro barbeiro;
    private PlanoMensalCliente contrato;

    @BeforeEach
    void preparar() {
        barbeiro = barbeiro("carteira-barbeiro");
        var usuario = new Usuario(); usuario.setLogin("carteira-cliente"); usuario.setSenha("hash");
        usuario.setAtivo(true); usuario.setRole(ROLE.CLIENTE); usuario = usuarios.save(usuario);
        var cliente = new Cliente(); cliente.setUsuario(usuario); cliente.setNome("Cliente");
        cliente.setTelefone("11999999999"); cliente = clientes.save(cliente);
        var plano = new PlanosMensal(); plano.setNomePlano("Plano"); plano.setDescricao("Plano teste");
        plano.setAtivo(true); plano.setValorMensal(new BigDecimal("100")); plano = planos.save(plano);
        contrato = new PlanoMensalCliente(); contrato.setCliente(cliente); contrato.setUsuario(usuario);
        contrato.setPlanoMensal(plano); contrato.setAtivo(true); contrato.setValorMensal(plano.getValorMensal());
        contrato = contratos.save(contrato);
    }

    @Test
    void somaHorasPontosELiquidoSemDuplicarAtendimentoMistoEIncluiDatasLimite() throws Exception {
        var misto = agendamento(barbeiro, "2026-01-01", 60, StatusAgendamento.CONCLUIDO);
        comissao(misto, "20.00"); pontos(misto, 30);
        comissao(agendamento(barbeiro, "2026-03-31", 45, StatusAgendamento.CONCLUIDO), "15.00");
        pontos(agendamento(barbeiro, "2026-02-10", 30, StatusAgendamento.CONCLUIDO), 30);
        // Todos estes registros devem ser ignorados, mesmo com comissao/pontos antigos.
        for (var status : new StatusAgendamento[] {StatusAgendamento.AGENDADO, StatusAgendamento.CANCELADO, StatusAgendamento.NAO_COMPARECEU}) {
            var a = agendamento(barbeiro, "2026-02-15", 100, status); comissao(a, "999"); pontos(a, 100);
        }
        for (String data : new String[] {"2025-12-31", "2026-04-01"}) {
            var a = agendamento(barbeiro, data, 100, StatusAgendamento.CONCLUIDO); comissao(a, "999"); pontos(a, 100);
        }
        var outro = agendamento(barbeiro("outro-carteira"), "2026-02-15", 100, StatusAgendamento.CONCLUIDO);
        comissao(outro, "999"); pontos(outro, 100);
        mvc.perform(get("/api/barbeiro/carteira").with(user("carteira-barbeiro").roles("BARBEIRO"))
                        .param("inicio", "2026-01-01").param("fim", "2026-03-31").param("barbeiroId", outro.getBarbeiro().getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.minutosTrabalhados").value(135))
                .andExpect(jsonPath("$.pontos").value(60)).andExpect(jsonPath("$.valorLiquido").value(35))
                .andExpect(jsonPath("$.inicio").value("2026-01-01")).andExpect(jsonPath("$.fim").value("2026-03-31"));
        extrato("pontos", 0).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPontos").value(60)).andExpect(jsonPath("$.totalAtendimentos").value(2))
                .andExpect(jsonPath("$.atendimentos[0].data").value("2026-02-10"))
                .andExpect(jsonPath("$.atendimentos[1].agendamentoId").value(misto.getId()))
                .andExpect(jsonPath("$.atendimentos[1].pontos").value(30));
        extrato("comissoes", 0).andExpect(status().isOk())
                .andExpect(jsonPath("$.valorLiquido").value(35)).andExpect(jsonPath("$.totalAtendimentos").value(2))
                .andExpect(jsonPath("$.atendimentos[0].data").value("2026-03-31"))
                .andExpect(jsonPath("$.atendimentos[1].agendamentoId").value(misto.getId()))
                .andExpect(jsonPath("$.atendimentos[1].valorComissao").value(20));

    }

    @Test
    void adminSomaTodosFiltraBarbeiroEMantemComparacaoETotalDeHoje() throws Exception {
        var a = agendamento(barbeiro, "2026-01-01", 60, StatusAgendamento.CONCLUIDO);
        comissao(a, "20"); pontos(a, 30);
        var outro = barbeiro("outro-admin-comissao");
        var b = agendamento(outro, "2026-01-31", 45, StatusAgendamento.CONCLUIDO);
        comissao(b, "15"); pontos(b, 10);
        comissao(agendamento(barbeiro, "2026-01-15", 100, StatusAgendamento.CANCELADO), "999");
        comissao(agendamento(barbeiro, LocalDate.now().toString(), 15, StatusAgendamento.CONCLUIDO), "7");
        mvc.perform(get("/api/admin/comissao").with(user("admin").roles("ADMIN"))
                        .param("inicio", "2026-01-01").param("fim", "2026-01-31"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.minutosTrabalhados").value(105))
                .andExpect(jsonPath("$.pontosPlano").value(40)).andExpect(jsonPath("$.valorComissoes").value(35))
                .andExpect(jsonPath("$.comissoesHoje").value(7)).andExpect(jsonPath("$.barbeiros.length()").value(2));
        mvc.perform(get("/api/admin/comissao").with(user("admin").roles("ADMIN"))
                        .param("inicio", "2026-01-01").param("fim", "2026-01-31").param("barbeiroId", outro.getId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.minutosTrabalhados").value(45))
                .andExpect(jsonPath("$.pontosPlano").value(10)).andExpect(jsonPath("$.valorComissoes").value(15))
                .andExpect(jsonPath("$.comissoesHoje").value(0)).andExpect(jsonPath("$.barbeiros.length()").value(2));
    }

    @Test
    void adminValidaPeriodoPermissoesEConsultaSemMovimento() throws Exception {
        mvc.perform(get("/api/admin/comissao").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.valorComissoes").value(0))
                .andExpect(jsonPath("$.pontosPlano").value(0)).andExpect(jsonPath("$.comissoesHoje").value(0));
        for (String fim : new String[]{"2025-12-31", "2026-04-01", "invalido"}) {
            mvc.perform(get("/api/admin/comissao").with(user("admin").roles("ADMIN"))
                            .param("inicio", "2026-01-01").param("fim", fim)).andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/admin/comissao").with(user("admin").roles("ADMIN")).param("barbeiroId", "999999"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/admin/comissao").with(user("outro").roles("BARBEIRO")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/admin/comissao").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(forwardedUrl("/client/index.html"));
    }

    @Test
    void periodoSemAtendimentoRetornaZeros() throws Exception {
        consulta("2026-01-01", "2026-01-01").andExpect(status().isOk())
                .andExpect(jsonPath("$.minutosTrabalhados").value(0)).andExpect(jsonPath("$.pontos").value(0))
                .andExpect(jsonPath("$.valorLiquido").value(0));
    }

    @Test
    void limitaTresMesesCalendarioInclusiveEmAnoBissexto() throws Exception {
        consulta("2024-02-29", "2024-05-28").andExpect(status().isOk());
        consulta("2024-02-29", "2024-05-29").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("O periodo deve ter no maximo 3 meses"));
        consulta("2026-01-31", "2026-04-29").andExpect(status().isOk());
        consulta("2026-01-31", "2026-04-30").andExpect(status().isBadRequest());
        consulta("2026-01-01", "2026-04-01").andExpect(status().isBadRequest());
    }

    @Test
    void rejeitaDatasInvertidasAusentesOuInvalidas() throws Exception {
        consulta("2026-02-02", "2026-02-01").andExpect(status().isBadRequest());
        consulta("2026-02-30", "2026-03-01").andExpect(status().isBadRequest());
        mvc.perform(get("/api/barbeiro/carteira").with(user("carteira-barbeiro").roles("BARBEIRO")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void restringeAcessoEAceitaAberturaDiretaDaTela() throws Exception {
        mvc.perform(get("/api/barbeiro/carteira").with(user("cliente").roles("CLIENTE"))
                        .param("inicio", "2026-01-01").param("fim", "2026-01-31"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/barbeiro/carteira").with(user("carteira-barbeiro").roles("BARBEIRO")))
                .andExpect(status().isOk()).andExpect(forwardedUrl("/client/index.html"));
        mvc.perform(get("/api/barbeiro/carteira").with(user("sem-cadastro").roles("BARBEIRO"))
                        .param("inicio", "2026-01-01").param("fim", "2026-01-31"))
                .andExpect(status().isNotFound());
    }

    @Test
    void extratosVaziosRejeitamPeriodoInvalidoEPaginacaoInvalida() throws Exception {
        for (String tipo : new String[] {"pontos", "comissoes"}) {
            extrato(tipo, 0).andExpect(status().isOk()).andExpect(jsonPath("$.totalAtendimentos").value(0))
                    .andExpect(jsonPath("$.atendimentos").isEmpty()).andExpect(jsonPath("$.totalPaginas").value(0));
            extrato(tipo, -1).andExpect(status().isBadRequest());
            extrato(tipo, 1).andExpect(status().isBadRequest());
            mvc.perform(get("/api/barbeiro/carteira/" + tipo).with(user("carteira-barbeiro").roles("BARBEIRO"))
                            .param("inicio", "2026-01-01").param("fim", "2026-04-01"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensagem").value("O periodo deve ter no maximo 3 meses"));
            mvc.perform(get("/api/barbeiro/carteira/" + tipo).with(user("cliente").roles("CLIENTE"))
                            .param("inicio", "2026-01-01").param("fim", "2026-03-31"))
                    .andExpect(status().isForbidden());
            mvc.perform(get("/barbeiro/carteira/" + tipo).with(user("carteira-barbeiro").roles("BARBEIRO")))
                    .andExpect(status().isOk()).andExpect(forwardedUrl("/client/index.html"));
        }
    }

    @Test
    void paginacaoMantemTotalDoPeriodoETemOrdenacaoEstavel() throws Exception {
        Agendamento primeiro = null;
        for (int n = 0; n < 21; n++) {
            var a = agendamento(barbeiro, "2026-02-10", 1, StatusAgendamento.CONCLUIDO);
            if (n == 0) primeiro = a;
            pontos(a, 1); comissao(a, "1.00");
        }
        for (String tipo : new String[] {"pontos", "comissoes"}) {
            extrato(tipo, 0).andExpect(status().isOk()).andExpect(jsonPath("$.atendimentos.length()").value(20))
                    .andExpect(jsonPath("$.totalAtendimentos").value(21)).andExpect(jsonPath("$.totalPaginas").value(2));
            extrato(tipo, 1).andExpect(status().isOk()).andExpect(jsonPath("$.atendimentos.length()").value(1))
                    .andExpect(jsonPath("$.atendimentos[0].agendamentoId").value(primeiro.getId()));
            extrato(tipo, 2).andExpect(status().isBadRequest());
        }
        extrato("pontos", 1).andExpect(jsonPath("$.totalPontos").value(21));
        extrato("comissoes", 1).andExpect(jsonPath("$.valorLiquido").value(21));
    }

    @Test
    void mostraServicosSeparadosDoMistoEUsaValoresRegistrados() throws Exception {
        var incluido = new Servico(); incluido.setNome("Corte combinado"); incluido.setDuracaoMinutos(30);
        incluido.setPreco(new BigDecimal("45")); incluido = servicos.save(incluido);
        var extra = new Servico(); extra.setNome("Hidratacao combinada"); extra.setDuracaoMinutos(15);
        extra.setPreco(new BigDecimal("40")); extra = servicos.save(extra);
        contrato.getPlanoMensal().setServicosIncluidos(new java.util.HashSet<>(java.util.List.of(incluido)));
        var a = agendamento(barbeiro, "2026-02-10", 45, StatusAgendamento.CONCLUIDO);
        a.setPlanoMensalCliente(contrato); a.setNomeCliente("Nome registrado");
        a.setServico(new java.util.HashSet<>(java.util.List.of(incluido, extra)));
        a.getServicosPlano().addAll(CalculoServicosPlano.calcular(a.getServico(), contrato.getPlanoMensal()));
        pontos(a, 30); comissao(a, "20.00");
        incluido.setNome("Nome alterado no catalogo"); extra.setPreco(new BigDecimal("999"));
        barbeiro.setPorcentagemComissao(99);
        agendamentos.flush();
        extrato("pontos", 0).andExpect(status().isOk())
                .andExpect(jsonPath("$.atendimentos[0].servicos.length()").value(1))
                .andExpect(jsonPath("$.atendimentos[0].servicos[0]").value("Corte combinado"))
                .andExpect(jsonPath("$.atendimentos[0].pontos").value(30))
                .andExpect(jsonPath("$.atendimentos[0].duracaoMinutos").value(45))
                .andExpect(jsonPath("$.atendimentos[0].nomeCliente").value("Nome registrado"));
        extrato("comissoes", 0).andExpect(status().isOk())
                .andExpect(jsonPath("$.atendimentos[0].servicos[0]").value("Hidratacao combinada"))
                .andExpect(jsonPath("$.atendimentos[0].valorBase").value(50))
                .andExpect(jsonPath("$.atendimentos[0].percentualAplicado").value(40))
                .andExpect(jsonPath("$.atendimentos[0].valorComissao").value(20))
                .andExpect(jsonPath("$.atendimentos[0].nomePlano").value("Plano"));
    }

    private org.springframework.test.web.servlet.ResultActions extrato(String tipo, int pagina) throws Exception {
        return mvc.perform(get("/api/barbeiro/carteira/" + tipo).with(user("carteira-barbeiro").roles("BARBEIRO"))
                .param("inicio", "2026-01-01").param("fim", "2026-03-31").param("pagina", String.valueOf(pagina)));
    }

    private org.springframework.test.web.servlet.ResultActions consulta(String inicio, String fim) throws Exception {
        return mvc.perform(get("/api/barbeiro/carteira").with(user("carteira-barbeiro").roles("BARBEIRO"))
                .param("inicio", inicio).param("fim", fim));
    }

    private Barbeiro barbeiro(String login) {
        var u = new Usuario(); u.setLogin(login); u.setSenha("hash"); u.setRole(ROLE.BARBEIRO); u.setAtivo(true);
        var b = new Barbeiro(); b.setNome(login); b.setTelefone("11999999999"); b.setUsuario(usuarios.save(u));
        return barbeiros.save(b);
    }

    private Agendamento agendamento(Barbeiro b, String data, int minutos, StatusAgendamento status) {
        var a = new Agendamento(); a.setBarbeiro(b); a.setData(LocalDate.parse(data));
        a.setHoraInicio(LocalTime.of(9, 0)); a.setHoraFinalizacao(LocalTime.of(9, 0).plusMinutes(minutos));
        a.setDuracaoServicoMinutos(minutos); a.setStatus(status); a.setPrecoTotal(BigDecimal.ZERO);
        a.setFinalizadoEm(LocalDateTime.of(2026, 4, 2, 10, 0));
        return agendamentos.save(a);
    }

    private void comissao(Agendamento a, String valor) {
        var c = new ComissaoBarbeiro(); c.setAgendamento(a); c.setBarbeiro(a.getBarbeiro());
        c.setTipo(TipoComissao.PADRAO_BARBEIRO); c.setValorBase(new BigDecimal("50"));
        c.setPercentualAplicado(new BigDecimal("40")); c.setValorComissao(new BigDecimal(valor));
        c.setCriadoEm(LocalDateTime.now()); comissoes.save(c);
    }

    private void pontos(Agendamento a, int valor) {
        var p = new ComissaoPlano(); p.setAgendamento(a); p.setBarbeiro(a.getBarbeiro());
        p.setPlanoMensal(contrato.getPlanoMensal()); p.setPlanoMensalCliente(contrato);
        p.setDataAgendamento(a.getData()); p.setPontos(valor); p.setCriadoEm(LocalDateTime.now()); pontos.save(p);
    }
}

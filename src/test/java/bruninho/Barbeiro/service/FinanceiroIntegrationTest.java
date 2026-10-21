package bruninho.Barbeiro.service;

import bruninho.Barbeiro.domain.*;
import bruninho.Barbeiro.repository.*;
import bruninho.Barbeiro.security.model.*;
import bruninho.Barbeiro.security.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:financeiro_test;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH")
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class FinanceiroIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired BarbeiroRepository barbeiros;
    @Autowired AgendamentoRepository agendamentos;
    @Autowired ComissaoBarbeiroRepository comissoes;
    @Autowired ServicoRepository servicos;

    @Test
    void somaRecebidosEComissoesPelaFinalizacaoComLimitesInclusivosSemDuplicarServicos() throws Exception {
        var usuario = new Usuario(); usuario.setLogin("financeiro-barbeiro"); usuario.setSenha("hash");
        usuario.setRole(ROLE.BARBEIRO); usuario.setAtivo(true); usuario = usuarios.save(usuario);
        var barbeiro = new Barbeiro(); barbeiro.setNome("Barbeiro Teste"); barbeiro.setTelefone("11999999999");
        barbeiro.setUsuario(usuario); barbeiro = barbeiros.save(barbeiro);
        var primeiro = criar(barbeiro, "2026-10-01T00:00:00", StatusAgendamento.CONCLUIDO, true, "100.00", "40.00");
        for (var nome : new String[]{"Corte", "Barba"}) {
            var servico = new Servico(); servico.setNome(nome); servico.setPreco(new BigDecimal("50.00"));
            servico.setDuracaoMinutos(30); primeiro.getServico().add(servicos.save(servico));
        }
        criar(barbeiro, "2026-10-20T23:59:59", StatusAgendamento.CONCLUIDO, true, "50.00", "20.00");
        criar(barbeiro, "2026-10-21T00:00:00", StatusAgendamento.CONCLUIDO, true, "999.00", "999.00");
        criar(barbeiro, "2026-09-30T23:59:59", StatusAgendamento.CONCLUIDO, true, "999.00", "999.00");
        criar(barbeiro, "2026-10-10T12:00:00", StatusAgendamento.CONCLUIDO, false, "999.00", "999.00");
        criar(barbeiro, "2026-10-10T12:00:00", StatusAgendamento.CONCLUIDO, true, "0.00", "0.00");
        for (var status : new StatusAgendamento[]{StatusAgendamento.AGENDADO, StatusAgendamento.CANCELADO, StatusAgendamento.NAO_COMPARECEU}) {
            criar(barbeiro, "2026-10-10T12:00:00", status, true, "999.00", "999.00");
        }
        mvc.perform(get("/api/admin/financeiro").with(user("admin").roles("ADMIN"))
                        .param("inicio", "2026-10-01").param("fim", "2026-10-20"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.valorBruto").value(150))
                .andExpect(jsonPath("$.despesas").value(60)).andExpect(jsonPath("$.valorLiquido").value(90))
                .andExpect(jsonPath("$.entradas.length()").value(2)).andExpect(jsonPath("$.saidas.length()").value(2))
                .andExpect(jsonPath("$.entradas[1].descricao").value("Barba + Corte"))
                .andExpect(jsonPath("$.saidas[0].barbeiro").value("Barbeiro Teste"));
    }

    @Test
    void periodoVazioRetornaZerosEValidaDatas() throws Exception {
        mvc.perform(get("/api/admin/financeiro").with(user("admin").roles("ADMIN"))
                        .param("inicio", "2026-01-01").param("fim", "2026-01-01"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.valorBruto").value(0))
                .andExpect(jsonPath("$.despesas").value(0)).andExpect(jsonPath("$.valorLiquido").value(0));
        for (String fim : new String[]{"2025-12-31", "2026-04-01", "invalido"}) {
            mvc.perform(get("/api/admin/financeiro").with(user("admin").roles("ADMIN"))
                            .param("inicio", "2026-01-01").param("fim", fim))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void restringeApiEPaginaAoAdministrador() throws Exception {
        for (String role : new String[]{"CLIENTE", "BARBEIRO"}) {
            for (String path : new String[]{"/api/admin/financeiro", "/admin/financeiro"}) {
                mvc.perform(get(path).with(user("outro").roles(role))).andExpect(status().isForbidden());
            }
        }
        mvc.perform(get("/admin/financeiro").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(forwardedUrl("/client/index.html"));
    }

    @Test
    void dashboardAgrupaSemanaMesEAnoESomaFinanceiroSemIncluirCancelados() throws Exception {
        var usuario = new Usuario(); usuario.setLogin("dashboard-barbeiro"); usuario.setSenha("hash");
        usuario.setRole(ROLE.BARBEIRO); usuario.setAtivo(true); usuario = usuarios.save(usuario);
        var barbeiro = new Barbeiro(); barbeiro.setNome("Barbeiro Dashboard"); barbeiro.setTelefone("11999999999");
        barbeiro.setUsuario(usuario); barbeiro = barbeiros.save(barbeiro);
        var primeiro = criar(barbeiro,"2026-01-01T10:00:00",StatusAgendamento.CONCLUIDO,true,"100","40");
        primeiro.setData(LocalDate.of(2026,1,1)); primeiro.setDuracaoServicoMinutos(30);
        var cancelado = criar(barbeiro,"2026-01-31T10:00:00",StatusAgendamento.CANCELADO,true,"999","999");
        cancelado.setData(LocalDate.of(2026,1,31));
        var naoPago = criar(barbeiro,"2026-02-03T10:00:00",StatusAgendamento.CONCLUIDO,false,"50","20");
        naoPago.setData(LocalDate.of(2026,2,3));
        mvc.perform(get("/api/admin/deshboard").with(user("admin").roles("ADMIN"))
                        .param("periodo","ano").param("referencia","2026-10-20"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.grafico.length()").value(12))
                .andExpect(jsonPath("$.grafico[0].quantidade").value(2)).andExpect(jsonPath("$.grafico[1].quantidade").value(1))
                .andExpect(jsonPath("$.grafico[2].quantidade").value(0)).andExpect(jsonPath("$.agendamentos").value(3))
                .andExpect(jsonPath("$.concluidos").value(2)).andExpect(jsonPath("$.cancelados").value(1))
                .andExpect(jsonPath("$.valorBruto").value(100)).andExpect(jsonPath("$.despesas").value(40))
                .andExpect(jsonPath("$.valorLiquido").value(60)).andExpect(jsonPath("$.caixa").isEmpty())
                .andExpect(jsonPath("$.destaques[0].nome").value("Barbeiro Dashboard"));
        mvc.perform(get("/api/admin/deshboard").with(user("admin").roles("ADMIN"))
                        .param("periodo","mes").param("referencia","2026-01-20"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.agrupamento").value("semana"))
                .andExpect(jsonPath("$.grafico.length()").value(5))
                .andExpect(jsonPath("$.grafico[0].data").value("2026-01-01"))
                .andExpect(jsonPath("$.grafico[1].data").value("2026-01-08"))
                .andExpect(jsonPath("$.grafico[0].quantidade").value(1))
                .andExpect(jsonPath("$.grafico[1].quantidade").value(0))
                .andExpect(jsonPath("$.grafico[4].data").value("2026-01-29"))
                .andExpect(jsonPath("$.grafico[4].quantidade").value(1))
                .andExpect(jsonPath("$.agendamentos").value(2));
        mvc.perform(get("/api/admin/deshboard").with(user("admin").roles("ADMIN"))
                        .param("periodo","mes").param("referencia","2026-02-10"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.grafico.length()").value(4))
                .andExpect(jsonPath("$.grafico[0].quantidade").value(1));
        mvc.perform(get("/api/admin/deshboard").with(user("admin").roles("ADMIN"))
                        .param("periodo","semana").param("referencia","2026-01-01"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.grafico.length()").value(7))
                .andExpect(jsonPath("$.inicio").value("2025-12-29")).andExpect(jsonPath("$.fim").value("2026-01-04"))
                .andExpect(jsonPath("$.agendamentos").value(1));
    }

    @Test
    void dashboardVazioValidaPeriodosEAcesso() throws Exception {
        mvc.perform(get("/api/admin/deshboard").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.agendamentos").value(0))
                .andExpect(jsonPath("$.valorLiquido").value(0)).andExpect(jsonPath("$.destaques[0].barbeiroId").isEmpty());
        mvc.perform(get("/api/admin/deshboard").with(user("admin").roles("ADMIN")).param("periodo","invalido"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/deshboard").with(user("admin").roles("ADMIN"))
                        .param("periodo","personalizado").param("inicio","2026-01-01").param("fim","2027-01-01"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/deshboard").with(user("admin").roles("ADMIN"))
                        .param("periodo","personalizado").param("inicio","2026-01-01").param("fim","2026-01-03"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.grafico.length()").value(3));
        mvc.perform(get("/api/admin/deshboard").with(user("outro").roles("BARBEIRO"))).andExpect(status().isForbidden());
        mvc.perform(get("/admin/dashboard").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(forwardedUrl("/client/index.html"));
    }

    private Agendamento criar(Barbeiro barbeiro, String quando, StatusAgendamento status, boolean pago,
                             String valor, String comissao) {
        var a = new Agendamento(); a.setBarbeiro(barbeiro); a.setNomeCliente("Cliente avulso");
        // A data do agendamento difere da finalizacao para verificar o criterio financeiro.
        a.setData(LocalDate.of(2026, 8, 1)); a.setFinalizadoEm(LocalDateTime.parse(quando));
        a.setStatus(status); a.setPagamentoRecebido(pago); a.setPrecoTotal(new BigDecimal(valor));
        a.setFormaPagamento(FormaPagamento.PIX); a = agendamentos.save(a);
        var c = new ComissaoBarbeiro(); c.setAgendamento(a); c.setBarbeiro(barbeiro);
        c.setValorComissao(new BigDecimal(comissao)); c.setCriadoEm(a.getFinalizadoEm());
        comissoes.save(c); return a;
    }
}

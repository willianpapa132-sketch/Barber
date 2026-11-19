package bruninho.Barbeiro.service;

import bruninho.Barbeiro.domain.*;
import bruninho.Barbeiro.repository.*;
import bruninho.Barbeiro.security.model.*;
import bruninho.Barbeiro.security.repository.UsuarioRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:gerenciamento_planos;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH")
@ActiveProfiles("test") @AutoConfigureMockMvc @Transactional
class GerenciamentoPlanosIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired ClienteRepository clientes;
    @Autowired PlanoMensalRepository planos;
    @Autowired PlanoMensalClienteRepository contratos;
    @Autowired RecebimentoPlanoRepository recebimentos;
    @Autowired ConfiguracaoBarbeariaRepository configs;
    @Autowired GerenciamentoPlanosService gestao;
    @Autowired ValidacaoAgendamentoService validacao;
    @Autowired AgendamentoRepository agendamentos;
    @Autowired BarbeiroRepository barbeiros;
    private PlanoMensalCliente contrato;
    private final LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
    private String base;
    @BeforeEach void preparar() {
        var config = configs.atual().orElseThrow(); config.setDiasToleranciaPagamentoPlano(3);
        var usuario = new Usuario(); usuario.setLogin("gestao-cliente"); usuario.setSenha("hash");
        usuario.setRole(ROLE.CLIENTE); usuario.setAtivo(true); usuario = usuarios.save(usuario);
        var cliente = new Cliente(); cliente.setNome("Cliente Plano"); cliente.setTelefone("11999999999");
        cliente.setUsuario(usuario); cliente = clientes.save(cliente);
        var plano = new PlanosMensal(); plano.setNomePlano("Premium"); plano.setDescricao("Plano teste");
        plano.setAtivo(true); plano.setValorMensal(new BigDecimal("99.90")); plano = planos.save(plano);
        contrato = new PlanoMensalCliente(); contrato.setCliente(cliente); contrato.setUsuario(usuario); contrato.setPlanoMensal(plano);
        contrato.setAtivo(true); contrato.setValorMensal(new BigDecimal("99.90")); contrato.setCriadoEm(hoje.minusMonths(2).atTime(10, 0));
        contrato.setProximoVencimento(hoje.minusDays(4)); contrato = contratos.save(contrato);
        base = "/api/admin/gerenciamento-planos/" + contrato.getId();
    }
    @Test void marcadorLegadoNaoPodeSerUsadoEmNovosRecebimentos(){
        var erro=assertThrows(org.springframework.web.server.ResponseStatusException.class,()->gestao.receber(contrato.getId(),contrato.getProximoVencimento(),hoje,FormaPagamento.LEGADO,null,"admin"));
        assertEquals(400,erro.getStatusCode().value());assertTrue(erro.getReason().contains("forma de pagamento"));
    }

    @Test void clienteCancelaPlanoSemUsoEPodeContratarNovamenteSemApagarHistorico() throws Exception {
        mvc.perform(get("/api/cliente/planos").with(user("gestao-cliente").roles("CLIENTE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cancelamento.permitido").value(true));
        cancelarCliente().andExpect(status().isNoContent());
        assertFalse(contrato.getAtivo());
        mvc.perform(post("/api/cliente/planos/"+contrato.getPlanoMensal().getId()+"/contratar")
                .with(user("gestao-cliente").roles("CLIENTE")).contentType("application/json").content("{}"))
                .andExpect(status().isCreated());
        assertEquals(2,contratos.count());
        assertNotEquals(contrato.getId(),contratos.findByCliente_IdAndAtivoTrue(contrato.getCliente().getId()).orElseThrow().getId());
    }

    @Test void agendamentoAtivoBloqueiaAteClienteCancelarAgendamento() throws Exception {
        var agendamento=atendimento(StatusAgendamento.AGENDADO,hoje.plusDays(1).atTime(10,0));
        cancelarCliente().andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("agendamento ativo")));
        assertTrue(contrato.getAtivo());
        agendamento.setStatus(StatusAgendamento.CANCELADO);agendamentos.saveAndFlush(agendamento);
        cancelarCliente().andExpect(status().isNoContent());
    }

    @Test void atendimentoAposPagamentoExigeUltimaParcelaEHistoricoPermanece() throws Exception {
        pagamento(hoje.minusDays(3),hoje.minusMonths(1));
        contrato.setProximoVencimento(hoje.plusDays(5));
        var agendamento=atendimento(StatusAgendamento.CONCLUIDO,hoje.minusDays(1).atTime(10,0));
        cancelarCliente().andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("quite a última parcela")));
        gestao.receber(contrato.getId(),contrato.getProximoVencimento(),hoje,FormaPagamento.PIX,null,"admin");
        cancelarCliente().andExpect(status().isNoContent());
        assertEquals(2,recebimentos.findByContrato_IdOrderByVencimentoDesc(contrato.getId()).size());
        assertEquals(StatusAgendamento.CONCLUIDO,agendamentos.findById(agendamento.getId()).orElseThrow().getStatus());
    }

    @Test void vencimentoHojeBloqueiaMesmoComToleranciaELiberacaoAdministrativa() throws Exception {
        pagamento(hoje.minusDays(3),hoje.minusMonths(1));
        contrato.setProximoVencimento(hoje);contrato.setLiberadoAte(hoje.plusDays(10));
        configs.atual().orElseThrow().setDiasToleranciaPagamentoPlano(30);
        mvc.perform(get("/api/cliente/planos").with(user("gestao-cliente").roles("CLIENTE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cancelamento.permitido").value(false));
        cancelarCliente().andExpect(status().isBadRequest());assertTrue(contrato.getAtivo());
    }

    @Test void atendimentoSemPagamentoNaoPermiteCancelar() throws Exception {
        atendimento(StatusAgendamento.CONCLUIDO,hoje.minusDays(1).atTime(10,0));
        cancelarCliente().andExpect(status().isBadRequest());assertTrue(contrato.getAtivo());
    }

    @Test void perfilDoClienteNaoEnviaNemExigeFoto() throws Exception {
        mvc.perform(get("/api/cliente/perfil").with(user("gestao-cliente").roles("CLIENTE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.imgUrl").doesNotExist());
        mvc.perform(put("/api/cliente/perfil").with(user("gestao-cliente").roles("CLIENTE"))
                .contentType("application/json").content("{\"nome\":\"Cliente atualizado\",\"telefone\":\"11988887777\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Cliente atualizado"))
                .andExpect(jsonPath("$.imgUrl").doesNotExist());
    }

    private org.springframework.test.web.servlet.ResultActions cancelarCliente() throws Exception {
        return mvc.perform(post("/api/cliente/planos/cancelar").with(user("gestao-cliente").roles("CLIENTE")));
    }

    private void pagamento(LocalDate data,LocalDate vencimento) {
        var r=new RecebimentoPlano();r.setContrato(contrato);r.setDataPagamento(data);r.setVencimento(vencimento);
        r.setValor(contrato.getValorMensal());r.setFormaPagamento(FormaPagamento.PIX);
        r.setRegistradoEm(data.atTime(9,0));r.setRegistradoPor("admin");recebimentos.saveAndFlush(r);
    }

    private Agendamento atendimento(StatusAgendamento status,LocalDateTime data) {
        var u=new Usuario();u.setLogin("barbeiro-cancelamento");u.setSenha("hash");u.setAtivo(true);u.setRole(ROLE.BARBEIRO);usuarios.save(u);
        var b=new Barbeiro();b.setNome("Barbeiro");b.setTelefone("119999999");b.setUsuario(u);barbeiros.save(b);
        var a=new Agendamento();a.setCliente(contrato.getCliente());a.setBarbeiro(b);a.setPlanoMensalCliente(contrato);
        a.setNomeCliente(contrato.getCliente().getNome());a.setStatus(status);a.setData(data.toLocalDate());
        a.setHoraInicio(data.toLocalTime());a.setHoraFinalizacao(data.toLocalTime().plusMinutes(30));
        if(status==StatusAgendamento.CONCLUIDO)a.setFinalizadoEm(data.plusMinutes(30));
        return agendamentos.saveAndFlush(a);
    }
    @Test void bloqueioAutomaticoEDesbloqueioSemRegistrarPagamento() throws Exception {
        assertThrows(bruninho.Barbeiro.exception.BusinessException.class, () -> validacao.validarPagamentoPlano(contrato));
        gestao.atualizarBloqueios(); assertTrue(contrato.getPagamentoAtrasado());
        mvc.perform(get("/api/admin/gerenciamento-planos").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.contratos[0].status").value("BLOQUEADO"));
        mvc.perform(post(base + "/desbloqueio").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        assertFalse(contrato.getPagamentoAtrasado()); assertNotNull(contrato.getLiberadoAte());
        assertDoesNotThrow(() -> validacao.validarPagamentoPlano(contrato));
        assertEquals(hoje.minusDays(4), contrato.getProximoVencimento());
        assertTrue(recebimentos.findByContrato_IdOrderByVencimentoDesc(contrato.getId()).isEmpty());
    }
    @Test void recebimentoAvancaCobrancaUmaVezEntraNoFinanceiroEPreservaHistoricoAposCancelar() throws Exception {
        String body = "{\"vencimento\":\"" + contrato.getProximoVencimento() + "\",\"dataPagamento\":\"" + hoje
                + "\",\"formaPagamento\":\"PIX\",\"referencia\":\"mensalidade\"}";
        mvc.perform(post(base + "/recebimentos").with(user("admin").roles("ADMIN")).contentType("application/json").content(body))
                .andExpect(status().isOk());
        mvc.perform(post(base + "/recebimentos").with(user("admin").roles("ADMIN")).contentType("application/json").content(body))
                .andExpect(status().isConflict());
        assertFalse(contrato.getPagamentoAtrasado());
        mvc.perform(get("/api/admin/financeiro").with(user("admin").roles("ADMIN"))
                        .param("inicio", hoje.toString()).param("fim", hoje.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.valorBruto").value(99.90))
                .andExpect(jsonPath("$.entradas[0].tipo").value("Plano Mensal"));
        mvc.perform(post(base + "/cancelamento").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        assertFalse(contrato.getAtivo());
        mvc.perform(get(base + "/recebimentos").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].valor").value(99.90))
                .andExpect(jsonPath("$[0].registradoPor").value("admin"));
        mvc.perform(post(base + "/desbloqueio").with(user("admin").roles("ADMIN"))).andExpect(status().isBadRequest());
    }
    @Test void pagarUmaParcelaNaoQuitaTodasAsParcelasAntigas() throws Exception {
        contrato.setProximoVencimento(hoje.minusMonths(2));
        gestao.receber(contrato.getId(), contrato.getProximoVencimento(), hoje, FormaPagamento.DINHEIRO, null, "admin");
        assertTrue(contrato.getPagamentoAtrasado());
        assertThrows(bruninho.Barbeiro.exception.BusinessException.class, () -> validacao.validarPagamentoPlano(contrato));
    }
    @Test void toleranciaValidacaoEPermissoes() throws Exception {
        gestao.atualizarBloqueios(); assertTrue(contrato.getPagamentoAtrasado());
        mvc.perform(put("/api/admin/gerenciamento-planos/tolerancia").with(user("admin").roles("ADMIN"))
                .contentType("application/json").content("{\"dias\":5}")).andExpect(status().isOk());
        assertEquals(5, configs.atual().orElseThrow().getDiasToleranciaPagamentoPlano());
        assertFalse(contrato.getPagamentoAtrasado());
        assertDoesNotThrow(() -> validacao.validarPagamentoPlano(contrato));
        for (int dias : new int[]{-1, 31}) {
            mvc.perform(put("/api/admin/gerenciamento-planos/tolerancia").with(user("admin").roles("ADMIN"))
                    .contentType("application/json").content("{\"dias\":" + dias + "}")).andExpect(status().isBadRequest());
        }
        for (String role : new String[]{"CLIENTE", "BARBEIRO"}) {
            mvc.perform(get("/api/admin/gerenciamento-planos").with(user("outro").roles(role))).andExpect(status().isForbidden());
            mvc.perform(post(base + "/recebimentos").with(user("outro").roles(role))).andExpect(status().isForbidden());
            mvc.perform(post(base + "/cancelamento").with(user("outro").roles(role))).andExpect(status().isForbidden());
            mvc.perform(post(base + "/desbloqueio").with(user("outro").roles(role))).andExpect(status().isForbidden());
        }
        mvc.perform(get("/admin/gerenciamento-planos").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(forwardedUrl("/client/index.html"));
    }
}

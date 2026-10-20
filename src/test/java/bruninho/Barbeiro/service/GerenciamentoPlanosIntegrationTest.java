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
    private PlanoMensalCliente contrato;
    private final LocalDate hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
    private String base;
    @BeforeEach void preparar() {
        var config = configs.findById(1L).orElseThrow(); config.setDiasToleranciaPagamentoPlano(3);
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
        assertEquals(5, configs.findById(1L).orElseThrow().getDiasToleranciaPagamentoPlano());
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

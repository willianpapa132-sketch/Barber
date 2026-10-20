package bruninho.Barbeiro.service;

import bruninho.Barbeiro.Controller.Admin.cadastros.planomensal.*;
import bruninho.Barbeiro.domain.*;
import bruninho.Barbeiro.repository.*;
import bruninho.Barbeiro.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @ActiveProfiles("test") @Transactional @AutoConfigureMockMvc
class PlanoMensalServiceTest {
    @Autowired PlanoMensalService service;
    @Autowired PlanoMensalRepository planos;
    @Autowired ServicoRepository servicos;
    @Autowired PlanoMensalClienteRepository contratos;
    @Autowired ClienteRepository clientes;
    @Autowired bruninho.Barbeiro.security.repository.UsuarioRepository usuarios;
    @Autowired EntityManager entityManager;
    @Autowired MockMvc mvc;
    private static final String API = "/api/admin/planos-mensais";

    private Servico servico(String nome) {
        var s = new Servico(); s.setNome(nome); s.setPreco(new BigDecimal("50.00")); s.setDuracaoMinutos(30);
        return servicos.saveAndFlush(s);
    }
    private SalvarPlanoMensalRequest request(Servico incluido, List<DescontoServicoPlanoRequest> descontos) {
        return new SalvarPlanoMensalRequest("Plano teste", "Corte mensal", new BigDecimal("99.90"), true, 7, Set.of(incluido.getId()), descontos);
    }
    private PlanoMensalCliente contrato(Long planoId) {
        var usuario = usuarios.findByLogin("admin").orElseThrow();
        var cliente = new Cliente(); cliente.setNome("Cliente"); cliente.setTelefone("11999999999"); cliente.setUsuario(usuario); clientes.saveAndFlush(cliente);
        var c = new PlanoMensalCliente(); c.setCliente(cliente); c.setUsuario(usuario); c.setPlanoMensal(planos.findById(planoId).orElseThrow());
        c.setAtivo(true); c.setValorMensal(new BigDecimal("99.90")); c.setDiasMaximoAntecedencia(7);
        return contratos.saveAndFlush(c);
    }
    @Test void cadastroSemDescontoCobraExtraPeloValorNormal() {
        var incluido = servico("Corte"); var extra = servico("Barba");
        var resposta = service.cadastrar(request(incluido, null));
        entityManager.flush(); entityManager.clear();
        var plano = planos.findById(resposta.id()).orElseThrow();
        assertTrue(plano.getDescontosServicos().isEmpty());
        var calculado = CalculoServicosPlano.calcular(servicos.findById(extra.getId()).orElseThrow(), plano);
        assertEquals(new BigDecimal("50.00"), calculado.getPrecoLiquido());
        assertEquals(0, calculado.getPorcentagemDesconto()); assertFalse(calculado.isInclusoNoPlano());
    }
    @Test void aplicaDescontoSomenteAosExtrasConfigurados() {
        var incluido = servico("Corte"); var extra = servico("Barba"); var normal = servico("Sobrancelha");
        var resposta = service.cadastrar(request(incluido, List.of(new DescontoServicoPlanoRequest(20, Set.of(extra.getId())))));
        entityManager.flush(); entityManager.clear();
        var plano = planos.findById(resposta.id()).orElseThrow();
        var comDesconto = CalculoServicosPlano.calcular(servicos.findById(extra.getId()).orElseThrow(), plano);
        assertEquals(new BigDecimal("40.00"), comDesconto.getPrecoLiquido());
        assertEquals(new BigDecimal("50.00"), CalculoServicosPlano.calcular(servicos.findById(normal.getId()).orElseThrow(), plano).getPrecoLiquido());
        assertEquals(new BigDecimal("0.00"), CalculoServicosPlano.calcular(servicos.findById(incluido.getId()).orElseThrow(), plano).getPrecoLiquido());
    }
    @Test void rejeitaServicoEmDuasRegras() {
        var incluido = servico("Corte"); var extra = servico("Barba");
        var request = request(incluido, List.of(new DescontoServicoPlanoRequest(10, Set.of(extra.getId())), new DescontoServicoPlanoRequest(20, Set.of(extra.getId()))));
        assertThrows(BusinessException.class, () -> service.cadastrar(request));
    }
    @Test void rejeitaDescontoParaServicoIncluido() {
        var incluido = servico("Corte");
        assertThrows(BusinessException.class, () -> service.cadastrar(request(incluido, List.of(new DescontoServicoPlanoRequest(20, Set.of(incluido.getId()))))));
    }
    @Test void rejeitaPercentualInvalidoEServicoInexistente() {
        var incluido = servico("Corte");
        assertThrows(BusinessException.class, () -> service.cadastrar(request(incluido, List.of(new DescontoServicoPlanoRequest(101, Set.of(incluido.getId()))))));
        assertThrows(BusinessException.class, () -> service.cadastrar(request(incluido, List.of(new DescontoServicoPlanoRequest(20, Set.of(Long.MAX_VALUE))))));
    }
    @Test void campoOmitidoPreservaDescontosEListaVaziaRemove() {
        var incluido = servico("Corte"); var extra = servico("Barba");
        var criado = service.cadastrar(request(incluido, List.of(new DescontoServicoPlanoRequest(20, Set.of(extra.getId())))));
        assertEquals(1, service.atualizar(criado.id(), request(incluido, null)).descontosServicos().size());
        service.atualizar(criado.id(), request(incluido, List.of()));
        entityManager.flush(); entityManager.clear();
        assertTrue(planos.findById(criado.id()).orElseThrow().getDescontosServicos().isEmpty());
        assertTrue(servicos.existsById(extra.getId()));
    }
    @Test void substituiRegrasDeDescontoSemDuplicar() {
        var incluido = servico("Corte"); var extra = servico("Barba");
        var criado = service.cadastrar(request(incluido, List.of(new DescontoServicoPlanoRequest(20, Set.of(extra.getId())))));
        service.atualizar(criado.id(), request(incluido, List.of(new DescontoServicoPlanoRequest(35, Set.of(extra.getId())))));
        entityManager.flush(); entityManager.clear();
        var regras = planos.findById(criado.id()).orElseThrow().getDescontosServicos();
        assertEquals(1, regras.size()); assertEquals(35, regras.iterator().next().getPorcentagemDesconto());
    }
    @Test void edicaoAtualizaContratosEBloqueiaExclusao() throws Exception {
        var incluido = servico("Corte"); var plano = service.cadastrar(request(incluido, List.of())); var c = contrato(plano.id());
        service.atualizar(plano.id(), new SalvarPlanoMensalRequest("Atualizado", "Novo valor", new BigDecimal("120.00"), false, 14, Set.of(incluido.getId()), List.of()));
        entityManager.flush(); entityManager.clear();
        var contrato = contratos.findById(c.getId()).orElseThrow();
        assertEquals(new BigDecimal("120.00"), contrato.getValorMensal()); assertEquals(14, contrato.getDiasMaximoAntecedencia());
        assertTrue(contrato.getAtivo()); assertFalse(service.consultar(plano.id()).ativo()); assertFalse(service.consultar(plano.id()).podeExcluir());
        mvc.perform(delete(API + "/" + plano.id()).with(user("admin").roles("ADMIN"))).andExpect(status().isConflict());
    }
    @Test void excluiPlanoSemContratosPreservandoServicos() {
        var incluido = servico("Corte"); var extra = servico("Barba");
        var plano = service.cadastrar(request(incluido, List.of(new DescontoServicoPlanoRequest(20, Set.of(extra.getId())))));
        service.excluir(plano.id()); entityManager.clear();
        assertFalse(planos.existsById(plano.id())); assertTrue(servicos.existsById(incluido.getId())); assertTrue(servicos.existsById(extra.getId()));
    }
    @Test void apiCriaConsultaEditaFiltraEExclui() throws Exception {
        var incluido = servico("Corte");
        var json = """
                {"nomePlano":"Plano API", "descricao":"Corte mensal", "valorMensal":99.90,
                 "ativo":true, "diasMaximoAntecedencia":7, "servicosId":[%d], "descontosServicos":[]}
                """.formatted(incluido.getId());
        mvc.perform(post(API).with(user("admin").roles("ADMIN")).contentType("application/json").content(json))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.descontosServicos").isEmpty()).andExpect(jsonPath("$.servicosIncluidos[0].nome").value("Corte"));
        var plano = planos.findByNomePlanoIgnoreCase("Plano API").orElseThrow();
        mvc.perform(get(API + "/" + plano.getId()).with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andExpect(jsonPath("$.nomePlano").value("Plano API"));
        mvc.perform(put(API + "/" + plano.getId()).with(user("admin").roles("ADMIN")).contentType("application/json").content(json.replace("true", "false")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ativo").value(false));
        mvc.perform(get(API).param("busca", "plano api").param("ativo", "false").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get(API).param("busca", "inexistente").with(user("admin").roles("ADMIN"))).andExpect(jsonPath("$.total").value(0));
        mvc.perform(delete(API + "/" + plano.getId()).with(user("admin").roles("ADMIN"))).andExpect(status().isNoContent());
    }
    @Test void apiValidaCamposPaginacaoENaoEncontrado() throws Exception {
        mvc.perform(post(API).with(user("admin").roles("ADMIN")).contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.mensagem").exists());
        mvc.perform(get(API).param("pagina", "-1").with(user("admin").roles("ADMIN"))).andExpect(status().isBadRequest());
        mvc.perform(get(API + "/99999999").with(user("admin").roles("ADMIN"))).andExpect(status().isNotFound());
    }
    @Test void apiEPaginaPermitemApenasAdmin() throws Exception {
        mvc.perform(get(API).with(user("cliente").roles("CLIENTE"))).andExpect(status().isForbidden());
        mvc.perform(get(API + "/servicos").with(user("barbeiro").roles("BARBEIRO"))).andExpect(status().isForbidden());
        mvc.perform(get("/admin/cadastros/planos-mensais").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(forwardedUrl("/client/index.html"));
    }
}

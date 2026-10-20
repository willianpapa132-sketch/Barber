package bruninho.Barbeiro.service;
import bruninho.Barbeiro.Controller.Admin.cadastros.servicos.SalvarServicoRequest;
import bruninho.Barbeiro.domain.*;
import bruninho.Barbeiro.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest @ActiveProfiles("test") @Transactional
class ServicoAdminIntegrationTest {
    @Autowired jakarta.persistence.EntityManager entityManager;
    @Autowired BarbeiroRepository barbeiros;
    @Autowired bruninho.Barbeiro.security.repository.UsuarioRepository usuarios;
    @Autowired ServicoService service;
    @Autowired ServicoRepository servicos;
    @Autowired PlanoMensalRepository planos;
    @Autowired ServicosPlanosDescontoRepository descontos;
    @Autowired AgendamentoRepository agendamentos;
    @Test void cadastraEditaExclui() {
        service.cadastrarServico(new SalvarServicoRequest(" Teste CRUD ", new BigDecimal("25.00"), 30, true));
        var s = servicos.findByNomeIgnoreCase("Teste CRUD").orElseThrow();
        assertTrue(s.isAtivo());
        service.atualizarServico(s.getId(), new SalvarServicoRequest("Editado", new BigDecimal("40.00"), 45, false));
        assertEquals(45, s.getDuracaoMinutos()); assertFalse(s.isAtivo());
        assertFalse(service.possuiVinculos(s.getId()));
        service.DeletarServicoPorID(s.getId()); assertFalse(servicos.existsById(s.getId()));
    }
    @Test void rejeitaDadosInvalidos() {
        assertThrows(ResponseStatusException.class, () -> service.cadastrarServico(new SalvarServicoRequest(" ", BigDecimal.ONE, 30, true)));
        assertThrows(ResponseStatusException.class, () -> service.cadastrarServico(new SalvarServicoRequest("Teste", BigDecimal.ONE, 0, true)));
        assertThrows(ResponseStatusException.class, () -> service.cadastrarServico(new SalvarServicoRequest("Teste", null, 30, true)));
        assertThrows(ResponseStatusException.class, () -> service.cadastrarServico(new SalvarServicoRequest("Teste", new BigDecimal("-1"), 30, true)));
        assertThrows(ResponseStatusException.class, () -> service.localizarServicoPorID(Long.MAX_VALUE));
    }
    private Servico novo() { var s = new Servico(); s.setNome("Vinculado"); s.setPreco(BigDecimal.TEN); s.setDuracaoMinutos(30); return servicos.saveAndFlush(s); }
    private Agendamento agendamento() {
        var b = new Barbeiro(); b.setNome("Teste"); b.setTelefone("11999999999"); b.setUsuario(usuarios.findByLogin("admin").orElseThrow()); barbeiros.saveAndFlush(b);
        var a = new Agendamento(); a.setBarbeiro(b); return a;
    }
    @Test void impedeExclusaoComAgendamento() {
        var s = novo(); var a = agendamento(); a.setServico(Set.of(s)); agendamentos.saveAndFlush(a);
        assertThrows(ResponseStatusException.class, () -> service.DeletarServicoPorID(s.getId()));
    }
    @Test void impedeExclusaoComHistoricoPlano() {
        var s = novo(); var a = agendamento(); var historico = new ServicoAgendadoPlano(); historico.setServicoId(s.getId()); historico.setNome(s.getNome());
        historico.setDuracaoMinutos(30); historico.setPrecoOriginal(BigDecimal.TEN); historico.setPrecoLiquido(BigDecimal.TEN); a.getServicosPlano().add(historico); agendamentos.saveAndFlush(a);
        assertThrows(ResponseStatusException.class, () -> service.DeletarServicoPorID(s.getId()));
    }
    @Test void impedeExclusaoComDesconto() {
        var s = novo(); var p = new PlanosMensal(); p.setNomePlano("Desconto"); p.setDescricao("Teste"); p.setValorMensal(BigDecimal.TEN); p.setAtivo(true); planos.saveAndFlush(p);
        var d = new ServicosPlanosDesconto(); d.setPlanoMensal(p); d.setPorcentagemDesconto(10); d.setServicos(Set.of(s)); descontos.saveAndFlush(d);
        assertTrue(service.possuiVinculos(s.getId()));
        assertThrows(ResponseStatusException.class, () -> service.DeletarServicoPorID(s.getId()));
    }
    @Test void impedeExclusaoComPlanoIncluido() {
        var s = novo(); var p = new PlanosMensal(); p.setNomePlano("Plano teste"); p.setDescricao("Teste"); p.setValorMensal(BigDecimal.TEN); p.setAtivo(true); p.setServicosIncluidos(Set.of(s)); planos.saveAndFlush(p);
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> service.DeletarServicoPorID(s.getId())).getStatusCode().value());
        assertTrue(servicos.existsById(s.getId()));
    }
    private PlanosMensal plano(String nome, Servico servico) {
        var p = new PlanosMensal();
        p.setNomePlano(nome); p.setDescricao("Teste de duracao");
        p.setValorMensal(BigDecimal.TEN); p.setAtivo(true);
        p.getServicosIncluidos().add(servico);
        return planos.saveAndFlush(p);
    }
    @Test void alteracaoDeDuracaoRefleteEmTodosOsPlanosEDescontos() {
        var servico = novo();
        var primeiro = plano("Primeiro", servico);
        var segundo = plano("Segundo", servico);
        segundo.setAtivo(false);
        var terceiro = plano("Com desconto", servico);
        terceiro.getServicosIncluidos().clear();
        var desconto = new ServicosPlanosDesconto();
        desconto.setPlanoMensal(terceiro); desconto.setPorcentagemDesconto(15);
        desconto.getServicos().add(servico); descontos.saveAndFlush(desconto);
        var ids = java.util.List.of(primeiro.getId(), segundo.getId());
        var descontoId = desconto.getId();
        var servicoId = servico.getId();

        service.atualizarServico(servicoId, new SalvarServicoRequest(servico.getNome(), servico.getPreco(), 45, true));
        entityManager.flush(); entityManager.clear();

        for (var id : ids) {
            var atualizado = planos.findById(id).orElseThrow();
            var incluido = atualizado.getServicosIncluidos().iterator().next();
            assertEquals(45, incluido.getDuracaoMinutos());
            assertEquals(45, CalculoServicosPlano.pontos(CalculoServicosPlano.calcular(Set.of(incluido), atualizado)));
        }
        var regra = descontos.findById(descontoId).orElseThrow();
        assertEquals(45, regra.getServicos().iterator().next().getDuracaoMinutos());
        assertEquals(15, regra.getPorcentagemDesconto());
    }
    @Test void novaDuracaoPreservaTempoEPontosDoAgendamentoAnterior() {
        var servico = novo(); var plano = plano("Historico", servico);
        var a = agendamento(); a.setServico(Set.of(servico));
        a.setDuracaoServicoMinutos(30);
        a.getServicosPlano().addAll(CalculoServicosPlano.calcular(Set.of(servico), plano));
        agendamentos.saveAndFlush(a);
        var agendamentoId = a.getId(); var servicoId = servico.getId(); var planoId = plano.getId();

        service.atualizarServico(servicoId, new SalvarServicoRequest(servico.getNome(), servico.getPreco(), 45, true));
        entityManager.flush(); entityManager.clear();

        var anterior = agendamentos.findById(agendamentoId).orElseThrow();
        assertEquals(30, anterior.getDuracaoServicoMinutos());
        assertEquals(30, anterior.getServicosPlano().getFirst().getDuracaoMinutos());
        assertEquals(30, CalculoServicosPlano.pontos(anterior.getServicosPlano()));
        var novosItens = CalculoServicosPlano.calcular(Set.of(servicos.findById(servicoId).orElseThrow()), planos.findById(planoId).orElseThrow());
        assertEquals(45, CalculoServicosPlano.pontos(novosItens));
    }
}

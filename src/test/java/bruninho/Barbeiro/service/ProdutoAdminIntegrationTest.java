package bruninho.Barbeiro.service;
import bruninho.Barbeiro.Controller.Admin.cadastros.produtos.SalvarProdutoRequest;
import bruninho.Barbeiro.repository.ProdutoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest @ActiveProfiles("test") @Transactional
class ProdutoAdminIntegrationTest {
    @Autowired ProdutoService service;
    @Autowired ProdutoRepository repository;
    @Autowired jakarta.persistence.EntityManager em;
    @Test void cadastraBuscaEditaExclui() {
        service.cadastrar(new SalvarProdutoRequest(" Pomada teste ", new BigDecimal("35.90"), 3, 12));
        em.flush(); em.clear();
        var page = repository.listarParaAdmin("POMADA TESTE", org.springframework.data.domain.PageRequest.of(0, 10));
        assertEquals(1, page.getTotalElements());
        var produto = page.getContent().getFirst();
        assertEquals("Pomada teste", produto.getNome());
        assertEquals(3, produto.getPeriodicidadeMeses());
        assertEquals(12, produto.getQuantidadeEstoque());
        assertEquals(new BigDecimal("35.90"), produto.getPreco());
        service.atualizar(produto.getId(), new SalvarProdutoRequest("Pomada editada", BigDecimal.ZERO, 6, 0));
        em.flush(); em.clear();
        var editado = service.localizarPorId(produto.getId());
        assertEquals(6, editado.getPeriodicidadeMeses()); assertEquals(0, editado.getQuantidadeEstoque());
        service.excluir(produto.getId()); em.flush(); em.clear();
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> service.localizarPorId(produto.getId())).getStatusCode().value());
    }
    @Test void rejeitaDadosInvalidos() {
        var requests = java.util.List.of(
            new SalvarProdutoRequest(" ", BigDecimal.ONE, 3, 1),
            new SalvarProdutoRequest("Teste", new BigDecimal("-1"), 3, 1),
            new SalvarProdutoRequest("Teste", new BigDecimal("1.001"), 3, 1),
            new SalvarProdutoRequest("Teste", BigDecimal.ONE, 0, 1),
            new SalvarProdutoRequest("Teste", BigDecimal.ONE, 3, -1),
            new SalvarProdutoRequest("Teste", BigDecimal.ONE, null, 1));
        for (var r : requests) assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.cadastrar(r)).getStatusCode().value());
    }
}

package bruninho.Barbeiro.service;
import bruninho.Barbeiro.Controller.Admin.cadastros.produtos.SalvarProdutoRequest;
import bruninho.Barbeiro.domain.Produto;
import bruninho.Barbeiro.repository.ProdutoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
@Service
public class ProdutoService {
    private final ProdutoRepository repository;
    public ProdutoService(ProdutoRepository repository) { this.repository = repository; }
    @Transactional(readOnly=true)
    public Produto localizarPorId(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto n\u00e3o encontrado."));
    }
    @Transactional
    public void cadastrar(SalvarProdutoRequest request) { salvar(new Produto(), request); }
    @Transactional
    public void atualizar(Long id, SalvarProdutoRequest request) {
        var produto = localizarPorId(id);
        salvar(produto, request);
    }
    private void salvar(Produto produto, SalvarProdutoRequest r) {
        if (r.nome() == null || r.nome().isBlank() || r.nome().length() > 255
                || r.preco() == null || r.preco().signum() < 0 || r.preco().scale() > 2
                || r.preco().precision() - r.preco().scale() > 10
                || r.periodicidadeMeses() == null || r.periodicidadeMeses() <= 0
                || r.quantidadeEstoque() == null || r.quantidadeEstoque() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe nome, pre\u00e7o n\u00e3o negativo com at\u00e9 duas casas decimais, periodicidade em meses maior que zero e estoque n\u00e3o negativo.");
        }
        produto.setNome(r.nome().trim()); produto.setPreco(r.preco());
        produto.setPeriodicidadeMeses(r.periodicidadeMeses()); produto.setQuantidadeEstoque(r.quantidadeEstoque());
        produto.setAtualizadoEm(LocalDateTime.now()); repository.save(produto);
    }
    @Transactional
    public void excluir(Long id) { repository.delete(localizarPorId(id)); }
}

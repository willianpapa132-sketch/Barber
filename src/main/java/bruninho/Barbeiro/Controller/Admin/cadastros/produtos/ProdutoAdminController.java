package bruninho.Barbeiro.Controller.Admin.cadastros.produtos;

import bruninho.Barbeiro.domain.Produto;
import bruninho.Barbeiro.service.ProdutoService;
import bruninho.Barbeiro.repository.ProdutoRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/produtos")
public class ProdutoAdminController {
    private final ProdutoService service;
    private final ProdutoRepository repository;

    public ProdutoAdminController(ProdutoService service, ProdutoRepository repository) {
        this.service = service; this.repository = repository;
    }
    private ProdutoAdminResponse item(Produto s) {
        return new ProdutoAdminResponse(s.getId(), s.getNome(), s.getPreco(),
                s.getPeriodicidadeMeses(), s.getQuantidadeEstoque(), s.getCriadoEm(), s.getPercentualComissao());
    }

    @GetMapping
    @org.springframework.transaction.annotation.Transactional(readOnly=true)
    public ListagemProdutosResponse listar(@RequestParam(defaultValue="") String busca, @RequestParam(defaultValue="0") int pagina) {
        if (pagina < 0) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "Página inválida.");
        var page = repository.listarParaAdmin(busca.trim(), PageRequest.of(pagina, 10, Sort.by("nome").and(Sort.by("id"))));
        return new ListagemProdutosResponse(page.getContent().stream().map(this::item).toList(), page.getTotalElements(), page.getTotalPages(), pagina);
    }

    @GetMapping("/{id}")
    public ProdutoAdminResponse consultar(@PathVariable Long id) {
        return item(service.localizarPorId(id));
    }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    @org.springframework.transaction.annotation.Transactional
    public void criar(@Valid @RequestBody SalvarProdutoRequest r) {
        service.cadastrar(r);
    }
    @PutMapping("/{id}")
    public void editar(@PathVariable Long id, @Valid @RequestBody SalvarProdutoRequest r) {
        service.atualizar(id, r);
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) { service.excluir(id); }
}

package bruninho.Barbeiro.Controller.Admin.cadastros.barbeiro;

import bruninho.Barbeiro.repository.BarbeiroRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.List;

@RestController
@RequestMapping("/api/admin/barbeiros")
public class BarbeiroAdminController {
    private final BarbeiroRepository barbeiros;
    private final bruninho.Barbeiro.service.CadastroBarbeiroAdminService cadastro;

    public BarbeiroAdminController(BarbeiroRepository barbeiros, bruninho.Barbeiro.service.CadastroBarbeiroAdminService cadastro) { this.barbeiros = barbeiros; this.cadastro = cadastro; }

    @GetMapping("/{id}")
    public BarbeiroDetalheResponse consultar(@PathVariable Long id) { return cadastro.consultar(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BarbeiroDetalheResponse criar(@jakarta.validation.Valid @RequestBody SalvarBarbeiroRequest request) { return cadastro.salvar(null, request); }

    @PutMapping("/{id}")
    public BarbeiroDetalheResponse editar(@PathVariable Long id, @jakarta.validation.Valid @RequestBody SalvarBarbeiroRequest request) { return cadastro.salvar(id, request); }

    @GetMapping
    @Transactional(readOnly = true)
    public Listagem listar(@RequestParam(defaultValue = "") String busca,
                          @RequestParam(required = false) Boolean ativo,
                          @RequestParam(defaultValue = "0") int pagina) {
        if (pagina < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pagina invalida");
        var resultado = barbeiros.listarParaAdmin(busca.trim(), ativo,
                PageRequest.of(pagina, 10, Sort.by("nome").ascending().and(Sort.by("id"))));
        var itens = resultado.map(b -> new Item(b.getId(), b.getNome(), b.getTelefone(), b.getImgurl(),
                b.getPorcentagemComissao(), b.isAtivo())).getContent();
        return new Listagem(itens, resultado.getTotalElements(), resultado.getTotalPages(), pagina);
    }

    public record Item(Long id, String nome, String telefone, String imgUrl, Integer comissao, boolean ativo) {}
    public record Listagem(List<Item> itens, long total, int totalPaginas, int pagina) {}
}

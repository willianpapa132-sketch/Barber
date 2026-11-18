package bruninho.Barbeiro.Controller.Admin.comanda;
import bruninho.Barbeiro.service.ComandaService;
import bruninho.Barbeiro.repository.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import jakarta.validation.Valid;
import java.util.*;
@RestController @RequestMapping("/api/admin/comandas")
public class ComandaAdminController {
    private final ComandaService service;
    private final ServicoRepository servicos;
    private final ProdutoRepository produtos;
    public ComandaAdminController(ComandaService s,ServicoRepository sr,ProdutoRepository pr){service=s;servicos=sr;produtos=pr;}
    @GetMapping public List<ComandaService.Resumo> listar(@RequestParam java.time.LocalDate de,@RequestParam java.time.LocalDate ate,@RequestParam(defaultValue="false") boolean encerradas){return service.listar(de,ate,encerradas);}
    @GetMapping("/agendamento/{id}") public ComandaResponse consultar(@PathVariable Long id){return service.consultar(id);}
    @PostMapping("/agendamento/{id}/previa") public ComandaResponse previa(@PathVariable Long id,@Valid @RequestBody FecharComandaRequest r){return service.previa(id,r);}
    @PostMapping("/agendamento/{id}/fechar") public ComandaResponse fechar(@PathVariable Long id,@Valid @RequestBody FecharComandaRequest r,Authentication auth){return service.fechar(id,r,auth.getName());}
    public record Opcao(Long id,String nome,java.math.BigDecimal preco,Integer estoque){}
    @GetMapping("/catalogo") @Transactional(readOnly=true)
    public Map<String,List<Opcao>> catalogo(){return Map.of("servicos",servicos.findAllByAtivoTrueOrderByNomeAsc().stream().map(s->new Opcao(s.getId(),s.getNome(),s.getPreco(),null)).toList(),
        "produtos",produtos.findAll(org.springframework.data.domain.Sort.by("nome","id")).stream().map(p->new Opcao(p.getId(),p.getNome(),p.getPreco(),p.getQuantidadeEstoque())).toList());}
}

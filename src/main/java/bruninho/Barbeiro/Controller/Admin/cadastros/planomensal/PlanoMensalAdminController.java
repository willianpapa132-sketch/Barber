package bruninho.Barbeiro.Controller.Admin.cadastros.planomensal;

import bruninho.Barbeiro.service.PlanoMensalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/planos-mensais")
public class PlanoMensalAdminController {
    private final PlanoMensalService service;

    public PlanoMensalAdminController(PlanoMensalService service) { this.service = service; }

    @GetMapping
    public ListagemPlanosMensaisResponse listar(@RequestParam(defaultValue = "") String busca,
            @RequestParam(required = false) Boolean ativo, @RequestParam(defaultValue = "0") int pagina) {
        return service.listar(busca, ativo, pagina);
    }

    @GetMapping("/servicos")
    public List<ServicoPlanoAdminResponse> servicos() { return service.listarServicos(); }

    @GetMapping("/{id}")
    public PlanoMensalAdminResponse consultar(@PathVariable Long id) { return service.consultar(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlanoMensalAdminResponse criar(@Valid @RequestBody SalvarPlanoMensalRequest request) {
        return service.cadastrar(request);
    }

    @PutMapping("/{id}")
    public PlanoMensalAdminResponse editar(@PathVariable Long id, @Valid @RequestBody SalvarPlanoMensalRequest request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) { service.excluir(id); }
}

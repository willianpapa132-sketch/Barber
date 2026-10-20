package bruninho.Barbeiro.Controller.Admin.cadastros.servicos;

import bruninho.Barbeiro.domain.Servico;
import bruninho.Barbeiro.service.ServicoService;
import bruninho.Barbeiro.repository.ServicoRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/servicos")
public class ServicoAdminController {
    private final ServicoService service;
    private final ServicoRepository repository;

    public ServicoAdminController(ServicoService service, ServicoRepository repository) {
        this.service = service; this.repository = repository;
    }
    private ServicoAdminResponse item(Servico s) {
        return new ServicoAdminResponse(s.getId(), s.getNome(), s.getPreco(),
                s.getDuracaoMinutos(), s.isAtivo(), s.getCriadoEm(), !service.possuiVinculos(s.getId()));
    }

    @GetMapping
    @org.springframework.transaction.annotation.Transactional(readOnly=true)
    public ListagemServicosResponse listar(@RequestParam(defaultValue="") String busca, @RequestParam(defaultValue="0") int pagina) {
        if (pagina < 0) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "Página inválida.");
        var page = repository.listarParaAdmin(busca.trim(), PageRequest.of(pagina, 10, Sort.by("nome").and(Sort.by("id"))));
        return new ListagemServicosResponse(page.getContent().stream().map(this::item).toList(), page.getTotalElements(), page.getTotalPages(), pagina);
    }

    @GetMapping("/{id}")
    public ServicoAdminResponse consultar(@PathVariable Long id) {
        return item(service.localizarServicoPorID(id));
    }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    @org.springframework.transaction.annotation.Transactional
    public void criar(@Valid @RequestBody SalvarServicoRequest r) {
        service.cadastrarServico(r);
    }
    @PutMapping("/{id}")
    public void editar(@PathVariable Long id, @Valid @RequestBody SalvarServicoRequest r) {
        service.atualizarServico(id, r);
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) { service.DeletarServicoPorID(id); }
}

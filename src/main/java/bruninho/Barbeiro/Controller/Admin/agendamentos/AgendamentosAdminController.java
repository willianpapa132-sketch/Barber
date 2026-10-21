package bruninho.Barbeiro.Controller.Admin.agendamentos;

import bruninho.Barbeiro.Controller.barbeiro.agenda.*;
import bruninho.Barbeiro.Controller.barbeiro.agenda.bloqueioeagendamento.*;
import bruninho.Barbeiro.domain.StatusAgendamento;
import bruninho.Barbeiro.repository.*;
import bruninho.Barbeiro.security.repository.UsuarioRepository;
import bruninho.Barbeiro.service.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/agendamentos")
public class AgendamentosAdminController {
    private final BarbeiroRepository barbeiros;
    private final GradeAgendaBarbeiroService grade;
    private final GestaoAgendaBarbeiroService gestao;
    private final GestaoAgendaBarbeiroController opcoes;
    private final AgendamentoRepository agendamentos;
    private final UsuarioRepository usuarios;

    public AgendamentosAdminController(BarbeiroRepository barbeiros, GradeAgendaBarbeiroService grade,
            GestaoAgendaBarbeiroService gestao, GestaoAgendaBarbeiroController opcoes,
            AgendamentoRepository agendamentos, UsuarioRepository usuarios) {
        this.barbeiros = barbeiros; this.grade = grade; this.gestao = gestao; this.opcoes = opcoes;
        this.agendamentos = agendamentos; this.usuarios = usuarios;
    }
    public record BarbeiroOpcao(Long id, String nome, boolean ativo) {}
    @GetMapping("/barbeiros")
    @Transactional(readOnly = true)
    public List<BarbeiroOpcao> barbeiros() {
        return barbeiros.findAll(Sort.by("nome", "id")).stream()
                .map(b -> new BarbeiroOpcao(b.getId(), b.getNome(), b.isAtivo())).toList();
    }
    private String login(Long id) {
        var b = barbeiros.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Barbeiro nao encontrado"));
        if (b.getUsuario() == null) throw new ResponseStatusException(HttpStatus.CONFLICT, "Barbeiro sem usuario cadastrado");
        return b.getUsuario().getLogin();
    }
    @GetMapping("/{barbeiroId}/grade")
    @Transactional(readOnly = true)
    public GradeAgendaBarbeiroResponse grade(@PathVariable Long barbeiroId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(required = false) StatusAgendamento status) {
        return grade.consultar(login(barbeiroId), data, status);
    }
    @GetMapping("/{barbeiroId}/clientes")
    public List<GestaoAgendaBarbeiroController.ClienteOpcao> clientes(@RequestParam(defaultValue = "") String busca) {
        return opcoes.clientes(busca);
    }
    @GetMapping("/{barbeiroId}/servicos")
    public List<GestaoAgendaBarbeiroController.ServicoOpcao> servicos() { return opcoes.servicos(); }
    @GetMapping("/{barbeiroId}/clientes/{clienteId}/servicos")
    public GestaoAgendaBarbeiroController.ServicosClienteResponse servicosCliente(@PathVariable Long clienteId) {
        return opcoes.servicosCliente(clienteId);
    }
    @PostMapping("/{barbeiroId}/agendamentos")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public GestaoAgendaBarbeiroService.CriacaoResponse agendar(@PathVariable Long barbeiroId,
            Authentication auth, @Valid @RequestBody CriarAtendimentoBarbeiroRequest request) {
        var criador = usuarios.findByLogin(auth.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        var resposta = gestao.criarAtendimento(login(barbeiroId), request);
        agendamentos.findById(resposta.id()).orElseThrow().setCriadoPorUsuario(criador);
        return resposta;
    }
    @PostMapping("/{barbeiroId}/bloqueios")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public GestaoAgendaBarbeiroService.CriacaoResponse bloquear(@PathVariable Long barbeiroId,
            @Valid @RequestBody CriarBloqueioAgendaRequest request) {
        return gestao.criarBloqueio(login(barbeiroId), request);
    }
    @DeleteMapping("/{barbeiroId}/bloqueios/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void remover(@PathVariable Long barbeiroId, @PathVariable Long id) {
        gestao.removerBloqueio(login(barbeiroId), id);
    }
}

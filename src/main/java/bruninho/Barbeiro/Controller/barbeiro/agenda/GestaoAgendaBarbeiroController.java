package bruninho.Barbeiro.Controller.barbeiro.agenda;

import bruninho.Barbeiro.Controller.barbeiro.agenda.bloqueioeagendamento.CriarAtendimentoBarbeiroRequest;
import bruninho.Barbeiro.Controller.barbeiro.agenda.bloqueioeagendamento.CriarBloqueioAgendaRequest;
import bruninho.Barbeiro.domain.StatusAgendamento;
import bruninho.Barbeiro.repository.ClienteRepository;
import bruninho.Barbeiro.repository.ServicoRepository;
import bruninho.Barbeiro.repository.PlanoMensalClienteRepository;
import bruninho.Barbeiro.service.GradeAgendaBarbeiroService;
import bruninho.Barbeiro.service.GestaoAgendaBarbeiroService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/barbeiro/agenda")
public class GestaoAgendaBarbeiroController {
    private final GradeAgendaBarbeiroService grade;
    private final GestaoAgendaBarbeiroService gestao;
    private final ClienteRepository clientes;
    private final ServicoRepository servicos;
    private final PlanoMensalClienteRepository planos;

    public GestaoAgendaBarbeiroController(GradeAgendaBarbeiroService grade, GestaoAgendaBarbeiroService gestao,
                                          ClienteRepository clientes, ServicoRepository servicos, PlanoMensalClienteRepository planos) {
        this.grade = grade; this.gestao = gestao; this.clientes = clientes; this.servicos = servicos;
        this.planos = planos;
    }

    @GetMapping("/grade")
    public GradeAgendaBarbeiroResponse grade(Authentication auth,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(required = false) StatusAgendamento status) {
        return grade.consultar(auth.getName(), data, status);
    }

    public record ClienteOpcao(Long id, String nome, String telefone) {}
    public record ServicoOpcao(Long id, String nome, BigDecimal preco, Integer duracaoMinutos,
                               boolean inclusoNoPlano, int porcentagemDesconto, BigDecimal precoLiquido) {}

    private ServicoOpcao opcao(bruninho.Barbeiro.domain.Servico servico, bruninho.Barbeiro.domain.PlanosMensal plano) {
        var item = bruninho.Barbeiro.service.CalculoServicosPlano.calcular(servico, plano);
        return new ServicoOpcao(servico.getId(), servico.getNome(), item.getPrecoOriginal(), servico.getDuracaoMinutos(),
                item.isInclusoNoPlano(), item.getPorcentagemDesconto(), item.getPrecoLiquido());
    }
    public record ServicosClienteResponse(boolean possuiPlano, String nomePlano, List<ServicoOpcao> servicos) {}

    @GetMapping("/clientes/{clienteId}/servicos")
    @Transactional(readOnly = true)
    public ServicosClienteResponse servicosCliente(@PathVariable Long clienteId) {
        if (!clientes.existsById(clienteId)) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente nao encontrado");
        }
        var contrato = planos.findByCliente_IdAndAtivoTrue(clienteId);
        if (contrato.isEmpty()) return new ServicosClienteResponse(false, null, servicos());
        var plano = contrato.get().getPlanoMensal();
        if (!Boolean.TRUE.equals(plano.getAtivo())) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "Plano mensal do cliente esta inativo");
        }
        var opcoes = servicos.findAllByAtivoTrueOrderByNomeAsc().stream().map(s -> opcao(s, plano)).toList();
        return new ServicosClienteResponse(true, plano.getNomePlano(), opcoes);
    }

    @GetMapping("/clientes")
    public List<ClienteOpcao> clientes(@RequestParam(defaultValue = "") String busca) {
        if (busca.length() > 80) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST, "Busca muito longa");
        return clientes.buscarParaAgenda(busca.trim(), PageRequest.of(0, 20)).stream()
                .map(c -> new ClienteOpcao(c.getId(), c.getNome(), c.getTelefone())).toList();
    }

    @GetMapping("/servicos")
    public List<ServicoOpcao> servicos() {
        return servicos.findAllByAtivoTrueOrderByNomeAsc().stream()
                .map(s -> opcao(s, null)).toList();
    }

    @PostMapping("/agendamentos")
    @ResponseStatus(HttpStatus.CREATED)
    public GestaoAgendaBarbeiroService.CriacaoResponse agendar(Authentication auth, @Valid @RequestBody CriarAtendimentoBarbeiroRequest request) {
        return gestao.criarAtendimento(auth.getName(), request);
    }

    @PostMapping("/bloqueios")
    @ResponseStatus(HttpStatus.CREATED)
    public GestaoAgendaBarbeiroService.CriacaoResponse bloquear(Authentication auth, @Valid @RequestBody CriarBloqueioAgendaRequest request) {
        return gestao.criarBloqueio(auth.getName(), request);
    }

    @DeleteMapping("/bloqueios/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(Authentication auth, @PathVariable Long id) {
        gestao.removerBloqueio(auth.getName(), id);
    }
}

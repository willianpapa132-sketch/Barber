package bruninho.Barbeiro.Controller.cliente.home.agendamentos;

import bruninho.Barbeiro.exception.NotFoundException;
import bruninho.Barbeiro.repository.BarbeiroRepository;
import bruninho.Barbeiro.repository.ClienteRepository;
import bruninho.Barbeiro.repository.PlanoMensalClienteRepository;
import bruninho.Barbeiro.repository.ServicoRepository;
import bruninho.Barbeiro.service.*;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.List;

@RestController
public class AgendamentoClienteController {
    private final AgendamentoService agendamentos;
    private final DisponibilidadeAgendamentosService disponibilidade;
    private final ClienteRepository clientes;
    private final BarbeiroRepository barbeiros;
    private final ServicoRepository servicos;
    private final PlanoMensalClienteRepository planosCliente;

    public AgendamentoClienteController(
            AgendamentoService agendamentos,
            DisponibilidadeAgendamentosService disponibilidade,
            ClienteRepository clientes,
            BarbeiroRepository barbeiros,
            ServicoRepository servicos,
            PlanoMensalClienteRepository planosCliente
    ) {
        this.agendamentos = agendamentos;
        this.disponibilidade = disponibilidade;
        this.clientes = clientes;
        this.barbeiros = barbeiros;
        this.servicos = servicos;
        this.planosCliente = planosCliente;
    }

    @GetMapping("/api/cliente/home/agendar")
    @Transactional(readOnly = true)
    public TelaAgendamentoResponse tela(Authentication authentication) {
        var cliente = clientes.findByUsuario_Login(authentication.getName())
                .orElseThrow(() -> new NotFoundException("Cliente nao encontrado"));

        var planoCliente = planosCliente.findByCliente_IdAndAtivoTrue(cliente.getId())
                .filter(plano -> Boolean.TRUE.equals(plano.getPlanoMensal().getAtivo()));

        var plano = planoCliente.map(p -> p.getPlanoMensal()).orElse(null);
        var servicosDisponiveis = servicos.findAllByAtivoTrueOrderByNomeAsc().stream()
                .map(servico -> ServicoAgendamentoResponse.de(servico, plano)).toList();

        var barbeirosDisponiveis = barbeiros.findAllByAtivoTrueOrderByNomeAsc().stream()
                .map(BarbeiroAgendamentoResponse::de)
                .toList();

        return new TelaAgendamentoResponse(
                cliente.getUsuario().getId(),
                cliente.getId(),
                planoCliente.isPresent(),
                planoCliente.map(contrato -> contrato.getPlanoMensal().getNomePlano()).orElse(null),
                barbeirosDisponiveis,
                servicosDisponiveis
        );
    }

    @PostMapping({"/agendamentos", "/api/cliente/home/agendar"})
    public ResponseEntity<AgendamentoResponse> criar(@Valid @RequestBody CriarAgendamentoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(AgendamentoResponse.de(agendamentos.agendar(request)));
    }

    @PostMapping({"/agendamentos/plano", "/api/cliente/home/agendar/plano"})
    public ResponseEntity<AgendamentoResponse> criarComPlano(@Valid @RequestBody CriarAgendamentoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(AgendamentoResponse.de(agendamentos.agendarComPlano(request)));
    }

    @GetMapping({"/agendamentos/disponibilidade/dias", "/api/cliente/home/agendar/disponibilidade/dias"})
    public List<LocalDate> dias(@RequestParam Long usuarioId, @RequestParam Long clienteId,
                               @RequestParam Long barbeiroId, @RequestParam List<Long> servicosIds) {
        return disponibilidade.diasDisponiveis(usuarioId, clienteId, barbeiroId, servicosIds);
    }

    @GetMapping({"/agendamentos/disponibilidade/horarios", "/api/cliente/home/agendar/disponibilidade/horarios"})
    public List<LocalTime> horarios(@RequestParam Long usuarioId, @RequestParam Long clienteId,
            @RequestParam Long barbeiroId, @RequestParam List<Long> servicosIds,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return disponibilidade.horariosDiaDisponivel(usuarioId, clienteId, barbeiroId, data, servicosIds);
    }
}

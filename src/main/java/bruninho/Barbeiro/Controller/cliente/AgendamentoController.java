package bruninho.Barbeiro.Controller.cliente;

import bruninho.Barbeiro.Controller.cliente.request.CriarAgendamentoRequest;
import bruninho.Barbeiro.service.*;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.List;

@RestController
@RequestMapping("/agendamentos")
public class AgendamentoController {
    private final AgendamentoService agendamentos;
    private final DisponibilidadeAgendamentosService disponibilidade;

    public AgendamentoController(AgendamentoService agendamentos, DisponibilidadeAgendamentosService disponibilidade) {
        this.agendamentos = agendamentos;
        this.disponibilidade = disponibilidade;
    }

    @PostMapping
    public ResponseEntity<AgendamentoResponse> criar(@Valid @RequestBody CriarAgendamentoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(AgendamentoResponse.de(agendamentos.agendar(request)));
    }

    @GetMapping("/disponibilidade/dias")
    public List<LocalDate> dias(@RequestParam Long usuarioId, @RequestParam Long clienteId,
                               @RequestParam Long barbeiroId, @RequestParam List<Long> servicosIds) {
        return disponibilidade.diasDisponiveis(usuarioId, clienteId, barbeiroId, servicosIds);
    }

    @GetMapping("/disponibilidade/horarios")
    public List<LocalTime> horarios(@RequestParam Long usuarioId, @RequestParam Long clienteId,
            @RequestParam Long barbeiroId, @RequestParam List<Long> servicosIds,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return disponibilidade.horariosDiaDisponivel(usuarioId, clienteId, barbeiroId, data, servicosIds);
    }
}

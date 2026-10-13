package bruninho.Barbeiro.service;

import bruninho.Barbeiro.Controller.cliente.agendamentos.AgendamentoClienteItemResponse;
import bruninho.Barbeiro.Controller.cliente.agendamentos.AgendamentoClienteResumoResponse;
import bruninho.Barbeiro.Controller.cliente.agendamentos.AgendamentosClientePageResponse;
import bruninho.Barbeiro.domain.StatusAgendamento;
import bruninho.Barbeiro.exception.NotFoundException;
import bruninho.Barbeiro.repository.AgendamentoRepository;
import bruninho.Barbeiro.repository.ClienteRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

@Service
public class AgendamentosClienteService {
    private final ClienteRepository clientes;
    private final AgendamentoRepository agendamentos;

    public AgendamentosClienteService(ClienteRepository clientes, AgendamentoRepository agendamentos) {
        this.clientes = clientes;
        this.agendamentos = agendamentos;
    }

    @Transactional(readOnly = true)
    public AgendamentosClientePageResponse listar(String login) {
        var cliente = clientes.findByUsuario_Login(login)
                .orElseThrow(() -> new NotFoundException("Cliente nao encontrado"));

        var hoje = LocalDate.now();
        var agora = LocalTime.now();

        var proximos = agendamentos.findProximosDoCliente(
                        cliente.getId(),
                        StatusAgendamento.AGENDADO,
                        hoje,
                        agora,
                        Pageable.unpaged()
                ).stream()
                .map(AgendamentoClienteResumoResponse::de)
                .toList();

        var antigos = agendamentos.findAntigosDoCliente(
                        cliente.getId(),
                        StatusAgendamento.AGENDADO,
                        hoje,
                        agora
                ).stream()
                .map(AgendamentoClienteResumoResponse::de)
                .toList();

        return new AgendamentosClientePageResponse(proximos, antigos);
    }

    @Transactional(readOnly = true)
    public AgendamentoClienteItemResponse detalhe(String login, Long agendamentoId) {
        var cliente = clientes.findByUsuario_Login(login)
                .orElseThrow(() -> new NotFoundException("Cliente nao encontrado"));

        return agendamentos.findByIdAndCliente_Id(agendamentoId, cliente.getId())
                .map(AgendamentoClienteItemResponse::de)
                .orElseThrow(() -> new NotFoundException("Agendamento nao encontrado"));
    }
}

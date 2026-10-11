package bruninho.Barbeiro.service;

import bruninho.Barbeiro.Controller.cliente.home.AgendamentoHomeResponse;
import bruninho.Barbeiro.Controller.cliente.home.ClienteHomeResponse;
import bruninho.Barbeiro.Controller.cliente.home.HomeResponseC;
import bruninho.Barbeiro.Controller.cliente.home.ProximoAgendamentoHomeResponse;
import bruninho.Barbeiro.domain.StatusAgendamento;
import bruninho.Barbeiro.exception.NotFoundException;
import bruninho.Barbeiro.repository.AgendamentoRepository;
import bruninho.Barbeiro.repository.ClienteRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

@Service
public class HomeClienteService {
    private final ClienteRepository clientes;
    private final AgendamentoRepository agendamentos;

    public HomeClienteService(
            ClienteRepository clientes,
            AgendamentoRepository agendamentos
    ) {
        this.clientes = clientes;
        this.agendamentos = agendamentos;
    }

    @Transactional(readOnly = true)
    public HomeResponseC homeDoCliente(String login) {
        var cliente = clientes.findByUsuario_Login(login)
                .orElseThrow(() -> new NotFoundException("Cliente nao encontrado"));

        var hoje = LocalDate.now();
        var agora = LocalTime.now();

        var proximoAgendamento = agendamentos.findProximosDoCliente(
                        cliente.getId(),
                        StatusAgendamento.AGENDADO,
                        hoje,
                        agora,
                        PageRequest.of(0, 1)
                ).stream()
                .findFirst()
                .map(ProximoAgendamentoHomeResponse::de)
                .orElse(null);

        return new HomeResponseC(
                ClienteHomeResponse.de(cliente),
                proximoAgendamento
        );
    }

    @Transactional(readOnly = true)
    public AgendamentoHomeResponse detalheAgendamento(String login, Long agendamentoId) {
        var cliente = clientes.findByUsuario_Login(login)
                .orElseThrow(() -> new NotFoundException("Cliente nao encontrado"));

        return agendamentos.findByIdAndCliente_Id(agendamentoId, cliente.getId())
                .map(AgendamentoHomeResponse::de)
                .orElseThrow(() -> new NotFoundException("Agendamento nao encontrado"));
    }
}

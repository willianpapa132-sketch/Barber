package bruninho.Barbeiro.service;

import bruninho.Barbeiro.Controller.barbeiro.agenda.AgendaBarbeiroResponse;
import bruninho.Barbeiro.Controller.barbeiro.agenda.AgendamentoAgendaBarbeiroResponse;
import bruninho.Barbeiro.domain.StatusAgendamento;
import bruninho.Barbeiro.repository.AgendamentoRepository;
import bruninho.Barbeiro.repository.BarbeiroRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class ConsultaAgendaBarbeiroService {
    private final BarbeiroRepository barbeiros;
    private final AgendamentoRepository agendamentos;

    public ConsultaAgendaBarbeiroService(BarbeiroRepository barbeiros, AgendamentoRepository agendamentos) {
        this.barbeiros = barbeiros;
        this.agendamentos = agendamentos;
    }

    @Transactional(readOnly = true)
    public AgendaBarbeiroResponse consultar(String login, LocalDate data, StatusAgendamento status,
                                            int pagina, int tamanho) {
        if (pagina < 0 || tamanho < 1 || tamanho > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Pagina deve ser maior ou igual a zero e tamanho deve estar entre 1 e 100");
        }
        var barbeiro = barbeiros.findByUsuario_Login(login)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Barbeiro nao encontrado"));
        var agora = LocalDateTime.now();
        var dataSelecionada = data == null ? agora.toLocalDate() : data;
        var resultado = agendamentos.findAgendaDoBarbeiro(barbeiro.getId(), dataSelecionada,
                status, PageRequest.of(pagina, tamanho));

        Long proximoId = null;
        if (dataSelecionada.equals(agora.toLocalDate())) {
            proximoId = agendamentos.findProximosDoBarbeiroNoDia(barbeiro.getId(), dataSelecionada,
                            agora.toLocalTime(), StatusAgendamento.AGENDADO, PageRequest.of(0, 1))
                    .stream().findFirst().map(agendamento -> agendamento.getId()).orElse(null);
        }
        return new AgendaBarbeiroResponse(
                dataSelecionada, status, proximoId,
                resultado.getContent().stream().map(AgendamentoAgendaBarbeiroResponse::de).toList(),
                resultado.getNumber(), resultado.getSize(), resultado.getTotalElements(),
                resultado.getTotalPages(), resultado.hasNext()
        );
    }
}

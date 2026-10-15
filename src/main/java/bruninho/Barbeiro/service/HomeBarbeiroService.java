package bruninho.Barbeiro.service;

import bruninho.Barbeiro.Controller.barbeiro.home.BarbeiroHomeResponse;
import bruninho.Barbeiro.Controller.barbeiro.home.ProximoAgendamentoBarbeiroResponse;
import bruninho.Barbeiro.domain.StatusAgendamento;
import bruninho.Barbeiro.repository.AgendamentoRepository;
import bruninho.Barbeiro.repository.BarbeiroRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class HomeBarbeiroService {
    private final BarbeiroRepository barbeiros;
    private final AgendamentoRepository agendamentos;

    public HomeBarbeiroService(BarbeiroRepository barbeiros, AgendamentoRepository agendamentos) {
        this.barbeiros = barbeiros;
        this.agendamentos = agendamentos;
    }

    @Transactional(readOnly = true)
    public BarbeiroHomeResponse homeDoBarbeiro(String login) {
        var barbeiro = barbeiros.findByUsuario_Login(login)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Barbeiro nao encontrado"));
        var agora = LocalDateTime.now();
        var hoje = agora.toLocalDate();
        var proximos = agendamentos.findProximosDoBarbeiroNoDia(
                        barbeiro.getId(), hoje, agora.toLocalTime(), StatusAgendamento.AGENDADO,
                        PageRequest.of(0, 3))
                .stream().map(ProximoAgendamentoBarbeiroResponse::de).toList();

        return new BarbeiroHomeResponse(
                new BarbeiroHomeResponse.BarbeiroResumo(barbeiro.getId(), barbeiro.getNome(), barbeiro.getImgurl()),
                hoje,
                agendamentos.countByBarbeiro_IdAndData(barbeiro.getId(), hoje),
                agendamentos.countByBarbeiro_IdAndDataAndStatus(barbeiro.getId(), hoje, StatusAgendamento.CONCLUIDO),
                agendamentos.countByBarbeiro_IdAndDataAndStatus(barbeiro.getId(), hoje, StatusAgendamento.CANCELADO),
                proximos
        );
    }
}

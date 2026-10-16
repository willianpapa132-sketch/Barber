package bruninho.Barbeiro.service;

import bruninho.Barbeiro.Controller.barbeiro.agenda.AgendamentoAgendaBarbeiroResponse;
import bruninho.Barbeiro.Controller.barbeiro.agenda.GradeAgendaBarbeiroResponse;
import bruninho.Barbeiro.Controller.barbeiro.agenda.GradeAgendaBarbeiroResponse.Periodo;
import bruninho.Barbeiro.domain.StatusAgendamento;
import bruninho.Barbeiro.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class GradeAgendaBarbeiroService {
    private final BarbeiroRepository barbeiros;
    private final JornadaBarbeiroRepository jornadas;
    private final HorarioFuncionamentoRepository funcionamento;
    private final AgendamentoRepository agendamentos;
    private final BloqueioBarbeiroRepository bloqueios;

    public GradeAgendaBarbeiroService(BarbeiroRepository barbeiros, JornadaBarbeiroRepository jornadas,
                                      HorarioFuncionamentoRepository funcionamento, AgendamentoRepository agendamentos,
                                      BloqueioBarbeiroRepository bloqueios) {
        this.barbeiros = barbeiros;
        this.jornadas = jornadas;
        this.funcionamento = funcionamento;
        this.agendamentos = agendamentos;
        this.bloqueios = bloqueios;
    }

    @Transactional(readOnly = true)
    public GradeAgendaBarbeiroResponse consultar(String login, LocalDate data, StatusAgendamento status) {
        var barbeiro = barbeiros.findByUsuario_Login(login)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Barbeiro nao encontrado"));
        var dia = data == null ? LocalDate.now() : data;
        var todos = agendamentos.findAllByBarbeiro_Usuario_LoginAndDataOrderByHoraInicioAsc(login, dia);
        var itens = todos.stream()
                .filter(a -> status == null || a.getStatus() == status)
                .map(AgendamentoAgendaBarbeiroResponse::de).toList();
        var jornada = jornadas.findByDiaSemanaAndBarbeiro_id(dia.getDayOfWeek(), barbeiro.getId()).orElse(null);
        var horario = funcionamento.findByDiaSemana(dia.getDayOfWeek());
        var periodos = new ArrayList<Periodo>();
        todos.stream().filter(a -> a.getStatus() != StatusAgendamento.CANCELADO && a.getStatus() != StatusAgendamento.NAO_COMPARECEU)
                .forEach(a -> periodos.add(new Periodo(null, "OCUPADO", "Horário com atendimento", a.getHoraInicio(), a.getHoraFinalizacao())));
        var registrosBloqueio = bloqueios.findAllByBarbeiro_IdAndDataBloqueioOrderByHoraInicioAsc(barbeiro.getId(), dia);
        for (var bloqueio : registrosBloqueio) {
            periodos.add(new Periodo(bloqueio.getId(), "BLOQUEIO", "Horário bloqueado", bloqueio.getHoraInicio(), bloqueio.getHoraFim()));
        }
        if (jornada == null || jornada.getHoraInicio() == null || jornada.getHoraFim() == null
                || !jornada.getHoraFim().isAfter(jornada.getHoraInicio())) {
            return new GradeAgendaBarbeiroResponse(dia, null, null, null, List.of(), itens, periodos, false,
                    "Sem jornada configurada para este dia.");
        }
        Integer intervalo = horario == null ? null : horario.getIntervaloMinimoEntreAgendamentos();
        if (intervalo == null || intervalo <= 0 || intervalo > 1440) {
            return new GradeAgendaBarbeiroResponse(dia, jornada.getHoraInicio(), jornada.getHoraFim(), null,
                    List.of(), itens, periodos, false, "Configure um intervalo de grade válido no funcionamento.");
        }
        boolean aberto = barbeiro.isAtivo() && Boolean.TRUE.equals(jornada.getAtivo())
                && !Boolean.TRUE.equals(jornada.getFolga()) && Boolean.FALSE.equals(horario.getFechado());
        var horarios = new ArrayList<LocalTime>();
        for (long segundo = jornada.getHoraInicio().toSecondOfDay(); segundo < jornada.getHoraFim().toSecondOfDay(); segundo += intervalo * 60L) {
            horarios.add(LocalTime.ofSecondOfDay(segundo));
        }
        if (!aberto) {
            periodos.add(new Periodo(null, "FECHADO", "Folga ou barbearia fechada", jornada.getHoraInicio(), jornada.getHoraFim()));
        } else {
            if (jornada.getIntervaloInicio() != null && jornada.getIntervaloFim() != null
                    && jornada.getIntervaloFim().isAfter(jornada.getIntervaloInicio())) {
                periodos.add(new Periodo(null, "INTERVALO", "Intervalo do barbeiro", jornada.getIntervaloInicio(), jornada.getIntervaloFim()));
            }
            if (horario.getHoraAbertura() != null && horario.getHoraAbertura().isAfter(jornada.getHoraInicio())) {
                periodos.add(new Periodo(null, "FECHADO", "Antes da abertura", jornada.getHoraInicio(), horario.getHoraAbertura()));
            }
            if (horario.getHoraFechamento() != null && horario.getHoraFechamento().isBefore(jornada.getHoraFim())) {
                periodos.add(new Periodo(null, "FECHADO", "Após o fechamento", horario.getHoraFechamento(), jornada.getHoraFim()));
            }
        }
        return new GradeAgendaBarbeiroResponse(dia, jornada.getHoraInicio(), jornada.getHoraFim(), intervalo,
                horarios, itens, periodos, aberto && !dia.isBefore(LocalDate.now()), aberto ? null : "Dia sem atendimento.");
    }
}

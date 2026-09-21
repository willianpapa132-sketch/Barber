package bruninho.Barbeiro.service;

import bruninho.Barbeiro.domain.Barbeiro;
import bruninho.Barbeiro.domain.BloqueioBarbeiro;
import bruninho.Barbeiro.domain.ConfiguracaoBarbearia;
import bruninho.Barbeiro.domain.HorarioFuncionamento;
import bruninho.Barbeiro.domain.JornadaBarbeiro;
import bruninho.Barbeiro.domain.Servico;
import bruninho.Barbeiro.domain.StatusAgendamento;
import bruninho.Barbeiro.repository.AgendamentoRepositorio;
import bruninho.Barbeiro.repository.BarbeiroRepositorio;
import bruninho.Barbeiro.repository.BloqueioBarbeiroRepositorio;
import bruninho.Barbeiro.repository.ConfiguracaoBarbeariaRepositorio;
import bruninho.Barbeiro.repository.HorarioFuncionamentoRepositorio;
import bruninho.Barbeiro.repository.JornadaBarbeiroRepositorio;
import bruninho.Barbeiro.repository.ServicoRepositorio;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DisponibilidadeServico {
    private static final List<StatusAgendamento> BLOQUEIAM_AGENDA = List.of(StatusAgendamento.AGENDADO, StatusAgendamento.CONCLUIDO);
    private final BarbeiroRepositorio barbeiros;
    private final ServicoRepositorio servicos;
    private final JornadaBarbeiroRepositorio jornadas;
    private final BloqueioBarbeiroRepositorio bloqueios;
    private final AgendamentoRepositorio agendamentos;
    private final ConfiguracaoBarbeariaRepositorio configuracoes;
    private final HorarioFuncionamentoRepositorio horariosFuncionamento;
    private final ZoneId zoneId;

    public DisponibilidadeServico(BarbeiroRepositorio barbeiros, ServicoRepositorio servicos,
                                  JornadaBarbeiroRepositorio jornadas, BloqueioBarbeiroRepositorio bloqueios,
                                  AgendamentoRepositorio agendamentos, ConfiguracaoBarbeariaRepositorio configuracoes,
                                  HorarioFuncionamentoRepositorio horariosFuncionamento, @Value("${app.zone}") String zone) {
        this.barbeiros = barbeiros;
        this.servicos = servicos;
        this.jornadas = jornadas;
        this.bloqueios = bloqueios;
        this.agendamentos = agendamentos;
        this.configuracoes = configuracoes;
        this.horariosFuncionamento = horariosFuncionamento;
        this.zoneId = ZoneId.of(zone);
    }

    public List<LocalTime> availableSlots(Long servicoId, Long barbeiroId, LocalDate data) {
        Servico servico = servicos.findById(servicoId).orElseThrow();
        List<LocalTime> horarios = new ArrayList<>();
        int diaSemana = data.getDayOfWeek().getValue();
        JornadaBarbeiro jornada = jornadaPara(barbeiroId, diaSemana);
        if (jornada == null) return horarios;
        LocalTime inicio = jornada.getHoraInicio();
        while (!inicio.plusMinutes(servico.getDuracaoMinutos()).isAfter(jornada.getHoraFim())) {
            if (isAvailable(servico, barbeiroId, LocalDateTime.of(data, inicio), false, null, jornada)) {
                horarios.add(inicio);
            }
            inicio = inicio.plusMinutes(15);
        }
        return horarios;
    }

    public void assertAvailable(Long servicoId, Long barbeiroId, LocalDateTime inicioEm, Long ignorarAgendamentoId) {
        Servico servico = servicos.findById(servicoId).orElseThrow(() -> new RegraNegocioException("Serviço não encontrado."));
        if (!isAvailable(servico, barbeiroId, inicioEm, true, ignorarAgendamentoId)) {
            throw new RegraNegocioException("Horário indisponível para o serviço e profissional selecionados.");
        }
    }

    public boolean isAvailable(Servico servico, Long barbeiroId, LocalDateTime inicioEm, boolean validarPassado) {
        return isAvailable(servico, barbeiroId, inicioEm, validarPassado, null, null);
    }

    private boolean isAvailable(Servico servico, Long barbeiroId, LocalDateTime inicioEm, boolean validarPassado, Long ignorarAgendamentoId) {
        return isAvailable(servico, barbeiroId, inicioEm, validarPassado, ignorarAgendamentoId, null);
    }

    private boolean isAvailable(Servico servico, Long barbeiroId, LocalDateTime inicioEm, boolean validarPassado, Long ignorarAgendamentoId, JornadaBarbeiro jornadaCarregada) {
        if (servico == null || !servico.isAtivo()) return false;
        Barbeiro barbeiro = barbeiros.findById(barbeiroId).orElse(null);
        if (barbeiro == null || !barbeiro.isAtivo()) return false;
        boolean atendeServico = barbeiro.getServicos().stream().anyMatch(s -> Objects.equals(s.getId(), servico.getId()) && s.isAtivo());
        if (!atendeServico) return false;

        ConfiguracaoBarbearia config = configuracoes.findById(1L).orElseThrow();
        LocalDateTime agora = LocalDateTime.now(zoneId);
        LocalDate data = inicioEm.toLocalDate();
        if (validarPassado && inicioEm.isBefore(agora.plusMinutes(config.getMinAntecedenciaMinutos()))) return false;
        if (data.isAfter(agora.toLocalDate().plusDays(config.getHorizonteDias()))) return false;
        if (inicioEm.getMinute() % 15 != 0 || inicioEm.getSecond() != 0 || inicioEm.getNano() != 0) return false;

        LocalDateTime fimEm = inicioEm.plusMinutes(servico.getDuracaoMinutos());
        int diaSemana = data.getDayOfWeek().getValue();
        HorarioFuncionamento funcionamento = horariosFuncionamento.findByDiaSemana(diaSemana).orElse(null);
        if (funcionamento == null || funcionamento.isFechado()) return false;
        if (inicioEm.toLocalTime().isBefore(funcionamento.getHoraAbertura()) || fimEm.toLocalTime().isAfter(funcionamento.getHoraFechamento())) return false;

        JornadaBarbeiro jornada = jornadaCarregada != null ? jornadaCarregada : jornadaPara(barbeiroId, diaSemana);
        if (jornada == null) return false;
        if (inicioEm.toLocalTime().isBefore(jornada.getHoraInicio()) || fimEm.toLocalTime().isAfter(jornada.getHoraFim())) return false;
        if (jornada.getIntervaloInicio() != null && jornada.getIntervaloFim() != null && sobrepoe(inicioEm.toLocalTime(), fimEm.toLocalTime(), jornada.getIntervaloInicio(), jornada.getIntervaloFim())) return false;

        for (BloqueioBarbeiro bloqueio : bloqueios.findByBarbeiroIdAndDataBloqueio(barbeiroId, data)) {
            LocalTime inicioBloqueio = bloqueio.getHoraInicio() == null ? LocalTime.MIN : bloqueio.getHoraInicio();
            LocalTime fimBloqueio = bloqueio.getHoraFim() == null ? LocalTime.MAX : bloqueio.getHoraFim();
            if (sobrepoe(inicioEm.toLocalTime(), fimEm.toLocalTime(), inicioBloqueio, fimBloqueio)) return false;
        }

        return agendamentos.findConflicts(barbeiroId, inicioEm, fimEm, BLOQUEIAM_AGENDA).stream()
            .allMatch(a -> Objects.equals(a.getId(), ignorarAgendamentoId));
    }

    private boolean sobrepoe(LocalTime inicio, LocalTime fim, LocalTime outroInicio, LocalTime outroFim) {
        return inicio.isBefore(outroFim) && fim.isAfter(outroInicio);
    }

    private JornadaBarbeiro jornadaPara(Long barbeiroId, int diaSemana) {
        return jornadas.findByBarbeiroIdOrderByDiaSemana(barbeiroId).stream()
            .filter(s -> s.isAtivo() && s.getDiaSemana() == diaSemana)
            .findFirst()
            .orElse(null);
    }
}

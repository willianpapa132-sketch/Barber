package bruninho.Barbeiro.Controller.cliente.home;

import bruninho.Barbeiro.domain.Agendamento;
import bruninho.Barbeiro.domain.Servico;
import bruninho.Barbeiro.domain.StatusAgendamento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public record AgendamentoHomeResponse(
        Long id,
        LocalDate data,
        String date,
        String dayName,
        String dayNumber,
        String month,
        LocalTime horaInicio,
        LocalTime horaFinalizacao,
        String startsAt,
        String endsAt,
        String barberName,
        String barberAvatarUrl,
        List<String> services,
        String details,
        StatusAgendamento status,
        String statusLabel,
        BigDecimal precoTotal
) {
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATE_LABEL = DateTimeFormatter.ofPattern("dd 'de' MMM. 'de' yyyy", PT_BR);
    private static final DateTimeFormatter DAY_NAME = DateTimeFormatter.ofPattern("EEE", PT_BR);
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMM", PT_BR);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    public static AgendamentoHomeResponse de(Agendamento agendamento) {
        var servicos = agendamento.getServico().stream()
                .map(Servico::getNome)
                .sorted()
                .toList();

        return new AgendamentoHomeResponse(
                agendamento.getId(),
                agendamento.getData(),
                agendamento.getData().format(DATE_LABEL),
                agendamento.getData().format(DAY_NAME).replace(".", "").toUpperCase(PT_BR),
                String.valueOf(agendamento.getData().getDayOfMonth()),
                agendamento.getData().format(MONTH).replace(".", "").toUpperCase(PT_BR),
                agendamento.getHoraInicio(),
                agendamento.getHoraFinalizacao(),
                agendamento.getHoraInicio().format(TIME),
                agendamento.getHoraFinalizacao().format(TIME),
                agendamento.getBarbeiro().getNome(),
                agendamento.getBarbeiro().getImgurl(),
                servicos,
                detalhes(servicos, agendamento.getDuracaoServicoMinutos()),
                agendamento.getStatus(),
                statusLabel(agendamento.getStatus()),
                agendamento.getPrecoTotal()
        );
    }

    private static String detalhes(List<String> servicos, int duracaoServicoMinutos) {
        if (servicos.size() > 1) {
            return String.join(" + ", servicos);
        }
        return duracaoServicoMinutos + " minutos";
    }

    private static String statusLabel(StatusAgendamento status) {
        return switch (status) {
            case AGENDADO -> "Agendado";
            case CONCLUIDO -> "Concluido";
            case CANCELADO -> "Cancelado";
            case NAO_COMPARECEU -> "Nao compareceu";
        };
    }
}

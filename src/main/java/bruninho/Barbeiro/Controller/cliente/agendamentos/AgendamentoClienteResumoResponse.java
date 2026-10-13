package bruninho.Barbeiro.Controller.cliente.agendamentos;

import bruninho.Barbeiro.domain.Agendamento;
import bruninho.Barbeiro.domain.Servico;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public record AgendamentoClienteResumoResponse(
        Long id,
        String dayName,
        String dayNumber,
        String startsAt,
        String endsAt,
        String barberName,
        String barberAvatarUrl,
        List<String> services,
        BigDecimal precoTotal
) {
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DAY_NAME = DateTimeFormatter.ofPattern("EEE", PT_BR);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    public static AgendamentoClienteResumoResponse de(Agendamento agendamento) {
        var servicos = agendamento.getServico().stream()
                .map(Servico::getNome)
                .sorted()
                .toList();

        return new AgendamentoClienteResumoResponse(
                agendamento.getId(),
                agendamento.getData().format(DAY_NAME).replace(".", "").toUpperCase(PT_BR),
                String.valueOf(agendamento.getData().getDayOfMonth()),
                agendamento.getHoraInicio().format(TIME),
                agendamento.getHoraFinalizacao().format(TIME),
                agendamento.getBarbeiro().getNome(),
                agendamento.getBarbeiro().getImgurl(),
                servicos,
                agendamento.getPrecoTotal()
        );
    }
}

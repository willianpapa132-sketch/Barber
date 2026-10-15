package bruninho.Barbeiro.Controller.barbeiro.home;

import bruninho.Barbeiro.domain.Agendamento;
import bruninho.Barbeiro.domain.Servico;
import bruninho.Barbeiro.domain.StatusAgendamento;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record ProximoAgendamentoBarbeiroResponse(
        Long id,
        LocalDate data,
        LocalTime horaInicio,
        LocalTime horaFinalizacao,
        int duracaoMinutos,
        String nomeCliente,
        String clienteImgUrl,
        List<String> servicos,
        StatusAgendamento status
) {
    public static ProximoAgendamentoBarbeiroResponse de(Agendamento agendamento) {
        var cliente = agendamento.getCliente();
        String nome = agendamento.getNomeCliente();
        if ((nome == null || nome.isBlank()) && cliente != null) {
            nome = cliente.getNome();
        }
        var servicos = agendamento.getServico().stream().map(Servico::getNome).sorted().toList();
        if (servicos.isEmpty() && agendamento.getNomeServicoPlano() != null
                && !agendamento.getNomeServicoPlano().isBlank()) {
            servicos = List.of(agendamento.getNomeServicoPlano());
        }
        return new ProximoAgendamentoBarbeiroResponse(
                agendamento.getId(), agendamento.getData(), agendamento.getHoraInicio(),
                agendamento.getHoraFinalizacao(), agendamento.getDuracaoServicoMinutos(),
                nome, cliente == null ? null : cliente.getImgUrl(), servicos, agendamento.getStatus()
        );
    }
}

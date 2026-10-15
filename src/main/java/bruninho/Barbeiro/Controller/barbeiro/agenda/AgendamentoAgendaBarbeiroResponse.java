package bruninho.Barbeiro.Controller.barbeiro.agenda;

import bruninho.Barbeiro.domain.Agendamento;
import bruninho.Barbeiro.domain.Servico;
import bruninho.Barbeiro.domain.StatusAgendamento;

import java.time.LocalTime;
import java.util.List;

public record AgendamentoAgendaBarbeiroResponse(
        Long id,
        LocalTime horaInicio,
        LocalTime horaFinalizacao,
        int duracaoMinutos,
        String nomeCliente,
        List<String> servicos,
        StatusAgendamento status
) {
    public static AgendamentoAgendaBarbeiroResponse de(Agendamento agendamento) {
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
        return new AgendamentoAgendaBarbeiroResponse(
                agendamento.getId(), agendamento.getHoraInicio(), agendamento.getHoraFinalizacao(),
                agendamento.getDuracaoServicoMinutos(), nome,
                servicos, agendamento.getStatus()
        );
    }
}

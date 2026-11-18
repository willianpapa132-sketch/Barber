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
        StatusAgendamento status, boolean semanaPagamentoPlano, java.time.LocalDate vencimentoPlano
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
        var plano = agendamento.getPlanoMensalCliente();
        var vencimento = plano == null ? null : plano.getProximoVencimento() != null
                ? plano.getProximoVencimento() : plano.getCriadoEm() == null ? null : plano.getCriadoEm().toLocalDate();
        boolean semanaPagamento = false;
        if (plano != null && Boolean.TRUE.equals(plano.getAtivo()) && vencimento != null
                && agendamento.getData() != null && agendamento.getStatus() == StatusAgendamento.AGENDADO) {
            var inicio = vencimento.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
            semanaPagamento = !agendamento.getData().isBefore(inicio) && !agendamento.getData().isAfter(inicio.plusDays(6));
        }
        return new AgendamentoAgendaBarbeiroResponse(
                agendamento.getId(), agendamento.getHoraInicio(), agendamento.getHoraFinalizacao(),
                agendamento.getDuracaoServicoMinutos(), nome,
                servicos, agendamento.getStatus(), semanaPagamento, vencimento
        );
    }
}

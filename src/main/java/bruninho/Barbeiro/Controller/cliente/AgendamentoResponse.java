package bruninho.Barbeiro.Controller.cliente;

import bruninho.Barbeiro.domain.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public record AgendamentoResponse(Long id, Long usuarioId, Long clienteId, Long barbeiroId,
        List<Long> servicosIds, LocalDate data, LocalTime horaInicio, LocalTime horaFinalizacao,
        int duracaoServicoMinutos, BigDecimal precoTotal, StatusAgendamento status) {
    public static AgendamentoResponse de(Agendamento a) {
        return new AgendamentoResponse(a.getId(), a.getCliente().getUsuario().getId(), a.getCliente().getId(),
                a.getBarbeiro().getId(), a.getServico().stream().map(Servico::getId).sorted().toList(),
                a.getData(), a.getHoraInicio(), a.getHoraFinalizacao(), a.getDuracaoServicoMinutos(),
                a.getPrecoTotal(), a.getStatus());
    }
}

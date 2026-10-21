package bruninho.Barbeiro.Controller.Admin.caixa;

import bruninho.Barbeiro.domain.MovimentoCaixa;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimentoCaixaResponse(Long id, String tipo, BigDecimal valor,
                                    LocalDateTime criadoEm, String observacao, Long agendamentoId) {
    public static MovimentoCaixaResponse de(MovimentoCaixa m) {
        return new MovimentoCaixaResponse(m.getId(), m.getTipo().name(), m.getValor(), m.getCriadoEm(),
                m.getObservacao(), m.getAgendamento() == null ? null : m.getAgendamento().getId());
    }
}

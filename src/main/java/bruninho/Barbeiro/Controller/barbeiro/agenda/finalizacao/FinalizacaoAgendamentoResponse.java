package bruninho.Barbeiro.Controller.barbeiro.agenda.finalizacao;

import bruninho.Barbeiro.domain.FormaPagamento;
import bruninho.Barbeiro.domain.StatusAgendamento;
import bruninho.Barbeiro.domain.TipoComissao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record FinalizacaoAgendamentoResponse(
        Long id,
        String nomeCliente,
        LocalDate data,
        LocalTime horaInicio,
        LocalTime horaTermino,
        int duracaoMinutos,
        StatusAgendamento status,
        boolean atendimentoMensal,
        List<ServicoResponse> servicos,
        BigDecimal valorTotalServicos,
        BigDecimal valorCobrar,
        FormaPagamento formaPagamento,
        List<FormaPagamento> formasPagamentoDisponiveis,
        ComissaoResponse comissao,
        boolean podeFinalizar,
        String motivoBloqueio,
        LocalDateTime finalizadoEm
) {
    public record ServicoResponse(Long id, String nome, BigDecimal preco) {}
    public record ComissaoResponse(TipoComissao tipo, BigDecimal valorBase,
                                   BigDecimal percentualAplicado, BigDecimal valorComissao) {}
}

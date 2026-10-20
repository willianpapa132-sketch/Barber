package bruninho.Barbeiro.Controller.Admin.gereciamentoPlanos;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public record GerenciamentoPlanosResponse(int diasTolerancia, LocalDate hoje, List<Contrato> contratos) {
    public record Contrato(Long id, String cliente, String telefone, Long planoId, String plano,
                           BigDecimal valorMensal, LocalDateTime contratadoEm, LocalDate proximoVencimento,
                           LocalDate prazoPagamento, long diasParaVencer, String status, boolean ativo,
                           boolean pagamentoAtrasado, LocalDate liberadoAte) {}
    public record Recebimento(Long id, LocalDate vencimento, LocalDate dataPagamento, BigDecimal valor,
                              String formaPagamento, LocalDateTime registradoEm, String registradoPor, String referencia) {}
}

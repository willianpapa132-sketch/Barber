package bruninho.Barbeiro.Controller.barbeiro.carteira.valoresFormulado;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record AtendimentoFormuladoResponse(Long agendamentoId, LocalDate data,
        LocalTime horaInicio, LocalTime horaFim, int duracaoMinutos, String nomeCliente,
        List<String> servicos, String nomePlano, Integer pontos,
        BigDecimal valorBase, BigDecimal percentualAplicado, BigDecimal valorComissao) {}

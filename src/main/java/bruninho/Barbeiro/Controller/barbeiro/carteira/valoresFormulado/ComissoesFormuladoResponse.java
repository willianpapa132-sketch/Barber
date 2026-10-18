package bruninho.Barbeiro.Controller.barbeiro.carteira.valoresFormulado;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ComissoesFormuladoResponse(LocalDate inicio, LocalDate fim, BigDecimal valorLiquido,
        long totalAtendimentos, int pagina, int totalPaginas, List<AtendimentoFormuladoResponse> atendimentos) {}

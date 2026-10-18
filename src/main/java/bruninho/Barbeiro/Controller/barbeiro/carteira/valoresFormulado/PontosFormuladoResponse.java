package bruninho.Barbeiro.Controller.barbeiro.carteira.valoresFormulado;

import java.time.LocalDate;
import java.util.List;

public record PontosFormuladoResponse(LocalDate inicio, LocalDate fim, long totalPontos,
        long totalAtendimentos, int pagina, int totalPaginas, List<AtendimentoFormuladoResponse> atendimentos) {}

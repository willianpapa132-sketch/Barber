package bruninho.Barbeiro.Controller.barbeiro.carteira;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CarteiraBarbeiroResponse(LocalDate inicio, LocalDate fim,
                                      long minutosTrabalhados, long pontos, BigDecimal valorLiquido) {}

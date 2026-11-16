package bruninho.Barbeiro.Controller.Admin.cadastros.produtos;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public record ProdutoAdminResponse(Long id, String nome, BigDecimal preco,
    int periodicidadeMeses, int quantidadeEstoque, LocalDateTime criadoEm, BigDecimal percentualComissao) {}

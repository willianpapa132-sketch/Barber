package bruninho.Barbeiro.Controller.Admin.cadastros.servicos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ServicoAdminResponse(
        Long id,
        String nome,
        BigDecimal preco,
        int duracaoMinutos,
        boolean ativo,
        LocalDateTime criadoEm,
        boolean podeExcluir
) {}

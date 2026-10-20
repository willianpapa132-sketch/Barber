package bruninho.Barbeiro.Controller.Admin.cadastros.planomensal;

import bruninho.Barbeiro.domain.Servico;
import java.math.BigDecimal;

public record ServicoPlanoAdminResponse(Long id, String nome, BigDecimal preco, Integer duracaoMinutos, boolean ativo) {
    public static ServicoPlanoAdminResponse de(Servico s) {
        return new ServicoPlanoAdminResponse(s.getId(), s.getNome(), s.getPreco(), s.getDuracaoMinutos(), s.isAtivo());
    }
}

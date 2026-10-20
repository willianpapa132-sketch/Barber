package bruninho.Barbeiro.Controller.Admin.cadastros.planomensal;

import java.math.BigDecimal;
import java.util.List;

public record PlanoMensalAdminResponse(
        Long id, String nomePlano, String descricao, BigDecimal valorMensal, boolean ativo,
        Integer diasMaximoAntecedencia, List<ServicoPlanoAdminResponse> servicosIncluidos,
        List<DescontoPlanoAdminResponse> descontosServicos, boolean podeExcluir
) {}

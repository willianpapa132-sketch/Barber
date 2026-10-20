package bruninho.Barbeiro.Controller.Admin.cadastros.planomensal;

import java.util.List;

public record DescontoPlanoAdminResponse(Integer porcentagemDesconto, List<ServicoPlanoAdminResponse> servicos) {}

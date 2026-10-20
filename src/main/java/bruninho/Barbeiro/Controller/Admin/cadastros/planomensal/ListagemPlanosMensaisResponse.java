package bruninho.Barbeiro.Controller.Admin.cadastros.planomensal;

import java.util.List;

public record ListagemPlanosMensaisResponse(List<PlanoMensalAdminResponse> itens, long total, int totalPaginas, int pagina) {}

package bruninho.Barbeiro.Controller.Admin.cadastros.servicos;

import java.util.List;

public record ListagemServicosResponse(
        List<ServicoAdminResponse> itens,
        long total,
        int totalPaginas,
        int pagina
) {}

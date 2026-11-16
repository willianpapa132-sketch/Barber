package bruninho.Barbeiro.Controller.Admin.cadastros.produtos;
import java.util.List;
public record ListagemProdutosResponse(List<ProdutoAdminResponse> itens, long total, int totalPaginas, int pagina) {}

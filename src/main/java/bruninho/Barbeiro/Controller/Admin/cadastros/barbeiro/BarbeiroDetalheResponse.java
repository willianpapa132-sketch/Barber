package bruninho.Barbeiro.Controller.Admin.cadastros.barbeiro;

import java.util.List;

public record BarbeiroDetalheResponse(Long id, String nome, String telefone, String login,
                                     Integer comissao, boolean ativo, String imgUrl,
                                     List<SalvarBarbeiroRequest.Dia> jornada) {}

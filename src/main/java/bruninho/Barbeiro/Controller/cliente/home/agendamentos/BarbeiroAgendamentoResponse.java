package bruninho.Barbeiro.Controller.cliente.home.agendamentos;

import bruninho.Barbeiro.domain.Barbeiro;

public record BarbeiroAgendamentoResponse(
        Long id,
        String nome,
        String telefone,
        String imgUrl
) {
    public static BarbeiroAgendamentoResponse de(Barbeiro barbeiro) {
        return new BarbeiroAgendamentoResponse(
                barbeiro.getId(),
                barbeiro.getNome(),
                barbeiro.getTelefone(),
                barbeiro.getImgurl()
        );
    }
}

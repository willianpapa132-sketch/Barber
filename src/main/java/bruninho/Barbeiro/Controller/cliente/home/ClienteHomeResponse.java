package bruninho.Barbeiro.Controller.cliente.home;

import bruninho.Barbeiro.domain.Cliente;

public record ClienteHomeResponse(
        Long id,
        String nome,
        String imgUrl
) {
    public static ClienteHomeResponse de(Cliente cliente) {
        return new ClienteHomeResponse(
                cliente.getId(),
                cliente.getNome(),
                cliente.getImgUrl()
        );
    }
}

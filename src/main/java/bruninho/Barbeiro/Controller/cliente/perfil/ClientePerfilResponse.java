package bruninho.Barbeiro.Controller.cliente.perfil;

import bruninho.Barbeiro.domain.Cliente;

public record ClientePerfilResponse(
        Long id,
        String nome,
        String telefone
) {
    public static ClientePerfilResponse de(Cliente cliente) {
        return new ClientePerfilResponse(
                cliente.getId(),
                cliente.getNome(),
                cliente.getTelefone()
        );
    }
}

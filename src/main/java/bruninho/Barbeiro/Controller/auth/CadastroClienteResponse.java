package bruninho.Barbeiro.Controller.auth;

public record CadastroClienteResponse(
        Long usuarioId,
        Long clienteId,
        String login,
        String nome
) {
}

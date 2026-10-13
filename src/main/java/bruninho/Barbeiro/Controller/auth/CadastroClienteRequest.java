package bruninho.Barbeiro.Controller.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CadastroClienteRequest(
        @NotBlank @Size(max = 120) String nome,
        @NotBlank @Size(max = 20) String telefone,
        @NotBlank @Email @Size(max = 120) String email,
        @NotBlank @Size(min = 6, max = 72) String senha,
        @NotBlank @Size(min = 6, max = 72) String confirmarSenha
) {
}

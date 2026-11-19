package bruninho.Barbeiro.Controller.cliente.perfil;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AtualizarPerfilClienteRequest(
        @NotBlank @Size(max = 120) String nome,
        @NotBlank @Size(max = 20) String telefone
) {
}

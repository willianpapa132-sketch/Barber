package bruninho.Barbeiro.Controller.cliente.perfil;

import bruninho.Barbeiro.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cliente/perfil")
public class ClientePerfilController {
    private final ClienteService clientes;

    public ClientePerfilController(ClienteService clientes) {
        this.clientes = clientes;
    }

    @GetMapping
    public ClientePerfilResponse buscar(Authentication authentication) {
        return clientes.buscarPerfil(authentication.getName());
    }

    @PutMapping
    public ClientePerfilResponse atualizar(
            Authentication authentication,
            @Valid @RequestBody AtualizarPerfilClienteRequest request
    ) {
        return clientes.atualizarPerfil(authentication.getName(), request);
    }
}

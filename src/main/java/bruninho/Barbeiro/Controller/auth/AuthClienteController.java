package bruninho.Barbeiro.Controller.auth;

import bruninho.Barbeiro.service.CadastroClienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/clientes")
public class AuthClienteController {
    private final CadastroClienteService cadastroClienteService;

    public AuthClienteController(CadastroClienteService cadastroClienteService) {
        this.cadastroClienteService = cadastroClienteService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CadastroClienteResponse cadastrar(@Valid @RequestBody CadastroClienteRequest request) {
        return cadastroClienteService.cadastrar(request);
    }
}

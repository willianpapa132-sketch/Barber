package bruninho.Barbeiro.Controller.cliente.planos;

import bruninho.Barbeiro.service.PlanoMensalClienteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cliente/planos")
public class PlanoClienteController {
    private final PlanoMensalClienteService planosCliente;

    public PlanoClienteController(PlanoMensalClienteService planosCliente) {
        this.planosCliente = planosCliente;
    }

    @GetMapping
    public PlanoClientePageResponse listar(Authentication authentication) {
        return planosCliente.buscarTelaPlanos(authentication.getName());
    }

    @PostMapping("/{planoId}/contratar")
    public ResponseEntity<PlanoContratadoResponse> contratar(
            @PathVariable Long planoId,
            Authentication authentication
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(planosCliente.contratarPlano(authentication.getName(), planoId));
    }
}

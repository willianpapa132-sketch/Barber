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
    public record Preferencia(@jakarta.validation.constraints.Positive Long barbeiroPreferidoId) {}
    private final PlanoMensalClienteService planosCliente;
    private final bruninho.Barbeiro.service.CancelamentoPlanoClienteService cancelamento;

    public PlanoClienteController(PlanoMensalClienteService planosCliente, bruninho.Barbeiro.service.CancelamentoPlanoClienteService cancelamento) {
        this.planosCliente = planosCliente;
        this.cancelamento=cancelamento;
    }

    @GetMapping
    public PlanoClientePageResponse listar(Authentication authentication) {
        return planosCliente.buscarTelaPlanos(authentication.getName());
    }

    @PostMapping("/cancelar")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<Void> cancelar(Authentication authentication) {
        cancelamento.cancelar(authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{planoId}/contratar")
    public ResponseEntity<PlanoContratadoResponse> contratar(
            @PathVariable Long planoId,
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody(required=false) Preferencia preferencia,
            Authentication authentication
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(planosCliente.contratarPlano(authentication.getName(), planoId,
                        preferencia == null ? null : preferencia.barbeiroPreferidoId()));
    }

    @org.springframework.web.bind.annotation.PutMapping("/preferencia")
    public PlanoContratadoResponse preferencia(Authentication authentication,
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody Preferencia preferencia) {
        return planosCliente.atualizarPreferencia(authentication.getName(), preferencia.barbeiroPreferidoId());
    }
}

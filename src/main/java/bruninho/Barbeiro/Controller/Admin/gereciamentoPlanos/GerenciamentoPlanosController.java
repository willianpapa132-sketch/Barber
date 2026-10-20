package bruninho.Barbeiro.Controller.Admin.gereciamentoPlanos;

import bruninho.Barbeiro.domain.FormaPagamento;
import bruninho.Barbeiro.service.GerenciamentoPlanosService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/gerenciamento-planos")
public class GerenciamentoPlanosController {
    private final GerenciamentoPlanosService service;
    public GerenciamentoPlanosController(GerenciamentoPlanosService service) { this.service = service; }
    public record Tolerancia(@NotNull @Min(0) @Max(30) Integer dias) {}
    public record Pagamento(@NotNull LocalDate vencimento, @NotNull LocalDate dataPagamento,
                            @NotNull FormaPagamento formaPagamento, @Size(max = 250) String referencia) {}
    @GetMapping public GerenciamentoPlanosResponse listar() { return service.listar(); }
    @PutMapping("/tolerancia") public void tolerancia(@Valid @RequestBody Tolerancia r) { service.tolerancia(r.dias()); }
    @GetMapping("/{id}/recebimentos") public List<GerenciamentoPlanosResponse.Recebimento> historico(@PathVariable Long id) { return service.historico(id); }
    @PostMapping("/{id}/recebimentos") public void receber(@PathVariable Long id, @Valid @RequestBody Pagamento r, Authentication auth) {
        service.receber(id, r.vencimento(), r.dataPagamento(), r.formaPagamento(), r.referencia(), auth.getName());
    }
    @PostMapping("/{id}/cancelamento") public void cancelar(@PathVariable Long id) { service.cancelar(id); }
    @PostMapping("/{id}/desbloqueio") public void desbloquear(@PathVariable Long id) { service.desbloquear(id); }
}

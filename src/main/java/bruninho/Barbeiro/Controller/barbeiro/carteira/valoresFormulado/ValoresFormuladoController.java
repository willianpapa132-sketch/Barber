package bruninho.Barbeiro.Controller.barbeiro.carteira.valoresFormulado;

import bruninho.Barbeiro.service.ValoresFormuladoService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/barbeiro/carteira")
public class ValoresFormuladoController {
    private final ValoresFormuladoService valores;
    public ValoresFormuladoController(ValoresFormuladoService valores) { this.valores = valores; }

    @GetMapping("/pontos")
    public PontosFormuladoResponse pontos(Authentication auth,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(defaultValue = "0") int pagina) {
        return valores.pontos(auth.getName(), inicio, fim, pagina);
    }

    @GetMapping("/comissoes")
    public ComissoesFormuladoResponse comissoes(Authentication auth,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(defaultValue = "0") int pagina) {
        return valores.comissoes(auth.getName(), inicio, fim, pagina);
    }
}

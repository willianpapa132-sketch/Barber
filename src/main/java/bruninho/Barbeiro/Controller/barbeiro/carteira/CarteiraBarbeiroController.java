package bruninho.Barbeiro.Controller.barbeiro.carteira;

import bruninho.Barbeiro.service.CarteiraBarbeiroService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/barbeiro/carteira")
public class CarteiraBarbeiroController {
    private final CarteiraBarbeiroService carteira;

    public CarteiraBarbeiroController(CarteiraBarbeiroService carteira) { this.carteira = carteira; }

    @GetMapping
    public CarteiraBarbeiroResponse consultar(Authentication auth,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return carteira.consultar(auth.getName(), inicio, fim);
    }
}

package bruninho.Barbeiro.Controller.barbeiro.carteira;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CarteiraBarbeiroPageController {
    @GetMapping("/barbeiro/carteira")
    public String pagina() { return "forward:/client/index.html"; }
}

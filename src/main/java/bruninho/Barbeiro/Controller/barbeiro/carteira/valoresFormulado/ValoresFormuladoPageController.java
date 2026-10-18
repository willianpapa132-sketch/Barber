package bruninho.Barbeiro.Controller.barbeiro.carteira.valoresFormulado;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ValoresFormuladoPageController {
    @GetMapping({"/barbeiro/carteira/pontos", "/barbeiro/carteira/comissoes"})
    public String pagina() { return "forward:/client/index.html"; }
}

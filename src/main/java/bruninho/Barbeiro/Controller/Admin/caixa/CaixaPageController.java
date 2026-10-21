package bruninho.Barbeiro.Controller.Admin.caixa;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CaixaPageController {
    @GetMapping("/admin/caixa/{id}/movimentacoes")
    public String movimentacoes() { return "forward:/client/index.html"; }
}
